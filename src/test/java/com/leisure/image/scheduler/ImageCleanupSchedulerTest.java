package com.leisure.image.scheduler;

import com.leisure.global.properties.S3Properties;
import com.leisure.member.repository.MemberRepository;
import com.leisure.post.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ImageCleanupSchedulerTest {

    private static final String BASE_URL = "https://cdn.test";

    @Mock
    private S3Client s3Client;

    @Mock
    private PostRepository postRepository;

    @Mock
    private MemberRepository memberRepository;

    private final S3Properties s3Properties =
            new S3Properties("leisure-images-prod", "ap-northeast-2", BASE_URL, Duration.ofMinutes(5));

    private ImageCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new ImageCleanupScheduler(s3Client, s3Properties, postRepository, memberRepository);
    }

    @Test
    @DisplayName("참조 없고 48시간 지난 이미지만 삭제한다 (참조·유예 내 이미지는 보존)")
    void deletesOnlyUnreferencedAndOld() {
        Instant old = Instant.now().minus(Duration.ofHours(72));
        Instant recent = Instant.now().minus(Duration.ofHours(1));

        ListObjectsV2Iterable postsPage = paginatorOf(List.of(
                object("thumbnails/ref.jpg", old),
                object("thumbnails/orphan-old.jpg", old),
                object("thumbnails/orphan-new.jpg", recent)
        ));
        ListObjectsV2Iterable profilesPage = paginatorOf(List.of(
                object("profiles/ref.png", old),
                object("profiles/orphan-old.png", old)
        ));
        ListObjectsV2Iterable contentsPage = paginatorOf(List.of());
        given(s3Client.listObjectsV2Paginator(argThat(prefixIs("thumbnails/")))).willReturn(postsPage);
        given(s3Client.listObjectsV2Paginator(argThat(prefixIs("profiles/")))).willReturn(profilesPage);
        given(s3Client.listObjectsV2Paginator(argThat(prefixIs("contents/")))).willReturn(contentsPage);

        given(postRepository.findAllThumbnailUrls()).willReturn(List.of(BASE_URL + "/thumbnails/ref.jpg"));
        given(memberRepository.findAllProfileImageUrls()).willReturn(List.of(BASE_URL + "/profiles/ref.png"));

        scheduler.cleanupOrphans();

        assertThat(capturedDeletedKeys(2))
                .containsExactlyInAnyOrder("thumbnails/orphan-old.jpg", "profiles/orphan-old.png");
    }

    @Test
    @DisplayName("미참조라도 48시간 안 지난 최근 업로드는 삭제하지 않는다 (유예 보호)")
    void keepsRecentUnreferenced() {
        Instant recent = Instant.now().minus(Duration.ofHours(1));

        ListObjectsV2Iterable postsPage = paginatorOf(List.of(object("thumbnails/just-uploaded.jpg", recent)));
        ListObjectsV2Iterable profilesPage = paginatorOf(List.of());
        ListObjectsV2Iterable contentsPage = paginatorOf(List.of());
        given(s3Client.listObjectsV2Paginator(argThat(prefixIs("thumbnails/")))).willReturn(postsPage);
        given(s3Client.listObjectsV2Paginator(argThat(prefixIs("profiles/")))).willReturn(profilesPage);
        given(s3Client.listObjectsV2Paginator(argThat(prefixIs("contents/")))).willReturn(contentsPage);

        given(postRepository.findAllThumbnailUrls()).willReturn(List.of());
        given(memberRepository.findAllProfileImageUrls()).willReturn(List.of());

        scheduler.cleanupOrphans();

        verify(s3Client, never()).deleteObject(any(Consumer.class));
    }

    @Test
    @DisplayName("본문 이미지는 content에 참조되면 보존하고, 참조 없는 것만 삭제한다")
    void keepsBodyImagesReferencedInContent() {
        Instant old = Instant.now().minus(Duration.ofHours(72));

        ListObjectsV2Iterable postsPage = paginatorOf(List.of());
        ListObjectsV2Iterable profilesPage = paginatorOf(List.of());
        ListObjectsV2Iterable contentsPage = paginatorOf(List.of(
                object("contents/used.jpg", old),
                object("contents/orphan.jpg", old)
        ));
        given(s3Client.listObjectsV2Paginator(argThat(prefixIs("thumbnails/")))).willReturn(postsPage);
        given(s3Client.listObjectsV2Paginator(argThat(prefixIs("profiles/")))).willReturn(profilesPage);
        given(s3Client.listObjectsV2Paginator(argThat(prefixIs("contents/")))).willReturn(contentsPage);

        given(postRepository.findAllContents())
                .willReturn(List.of("<p>본문<img src=\"" + BASE_URL + "/contents/used.jpg\"></p>"));

        scheduler.cleanupOrphans();

        assertThat(capturedDeletedKeys(1)).containsExactly("contents/orphan.jpg");
    }

    private static ArgumentMatcherPrefix prefixIs(String prefix) {
        return new ArgumentMatcherPrefix(prefix);
    }

    private record ArgumentMatcherPrefix(String prefix) implements org.mockito.ArgumentMatcher<ListObjectsV2Request> {
        @Override
        public boolean matches(ListObjectsV2Request request) {
            return request != null && prefix.equals(request.prefix());
        }
    }

    private static S3Object object(String key, Instant lastModified) {
        return S3Object.builder().key(key).lastModified(lastModified).build();
    }

    private static ListObjectsV2Iterable paginatorOf(List<S3Object> objects) {
        ListObjectsV2Response page = ListObjectsV2Response.builder().contents(objects).build();
        ListObjectsV2Iterable iterable = mock(ListObjectsV2Iterable.class);
        given(iterable.iterator()).willReturn(List.of(page).iterator());
        return iterable;
    }

    @SuppressWarnings("unchecked")
    private List<String> capturedDeletedKeys(int expectedCount) {
        ArgumentCaptor<Consumer<DeleteObjectRequest.Builder>> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(s3Client, times(expectedCount)).deleteObject(captor.capture());

        return captor.getAllValues().stream()
                .map(consumer -> {
                    DeleteObjectRequest.Builder builder = DeleteObjectRequest.builder();
                    consumer.accept(builder);
                    return builder.build().key();
                })
                .toList();
    }
}
