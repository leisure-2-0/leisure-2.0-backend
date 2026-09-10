package com.leisure.search.index;

import com.leisure.global.properties.SearchProperties;
import com.leisure.post.domain.Post;
import com.leisure.post.repository.PostRepository;
import com.leisure.search.engine.PostSearchDocument;
import com.leisure.search.engine.PostSearchRepository;
import com.leisure.tag.service.TagReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("증분 색인 (PostIndexService.syncChangedPosts)")
class PostIndexServiceTest {

    @Mock
    private SearchProperties searchProperties;

    @Mock
    private PostIndexDirtyRepository postIndexDirtyRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private TagReader tagReader;

    @Mock
    private PostSearchRepository postSearchRepository;

    @InjectMocks
    private PostIndexService postIndexService;

    // --- 헬퍼 -------------------------------------------------------------

    private PostIndexDirty dirty(long postId, long version) {
        // 엔티티는 mock 대신 실제 객체 + ReflectionTestUtils (같은 패키지라 protected 생성자 접근 가능)
        PostIndexDirty dirty = new PostIndexDirty();
        ReflectionTestUtils.setField(dirty, "postId", postId);
        ReflectionTestUtils.setField(dirty, "version", version);
        return dirty;
    }

    private Post publishedPost(long postId) {
        Post post = Post.startWriting(1L);
        ReflectionTestUtils.setField(post, "postId", postId);
        post.applyContent("제목", "본문", null, null);   // category/location 없음(null 방어 확인)
        post.publish();
        return post;
    }

    private void enabledWithBatch() {
        given(searchProperties.enabled()).willReturn(true);
        given(searchProperties.sync()).willReturn(new SearchProperties.Sync(300000L, 500));
    }

    // --- 테스트 -----------------------------------------------------------

    @Test
    @DisplayName("검색 비활성이면 더티 조회조차 안 한다")
    void disabledDoesNothing() {
        given(searchProperties.enabled()).willReturn(false);

        postIndexService.syncChangedPosts();

        verify(postIndexDirtyRepository, never()).findProcessable(any(), anyInt());
        verifyNoInteractions(postSearchRepository);
    }

    @Test
    @DisplayName("더티가 없으면 ES를 건드리지 않는다")
    void noDirtiesNoIndexing() {
        enabledWithBatch();
        given(postIndexDirtyRepository.findProcessable(any(), anyInt())).willReturn(List.of());

        postIndexService.syncChangedPosts();

        verifyNoInteractions(postSearchRepository);
    }

    @Test
    @DisplayName("공개글은 색인하고, 성공하면 version 가드로 더티를 제거한다")
    void publishedIsIndexedThenVersionGuardedDelete() {
        enabledWithBatch();
        given(postIndexDirtyRepository.findProcessable(any(), anyInt())).willReturn(List.of(dirty(10L, 3L)));
        given(postRepository.findByPostIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(publishedPost(10L)));
        given(tagReader.findTags(10L)).willReturn(List.of("강릉"));

        postIndexService.syncChangedPosts();

        verify(postSearchRepository).index(argThat((List<PostSearchDocument> docs) -> docs.size() == 1));
        verify(postIndexDirtyRepository).deleteIfVersionMatches(10L, 3L);  // version 가드로 제거
        verify(postIndexDirtyRepository, never()).markFailure(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("없거나(소프트삭제) 비공개면 ES에서 삭제 바구니로 보낸다")
    void missingOrUnpublishedGoesToDeleteBucket() {
        enabledWithBatch();
        given(postIndexDirtyRepository.findProcessable(any(), anyInt())).willReturn(List.of(dirty(20L, 1L)));
        given(postRepository.findByPostIdAndDeletedAtIsNull(20L)).willReturn(Optional.empty());  // 소프트삭제 등

        postIndexService.syncChangedPosts();

        verify(postSearchRepository).delete(argThat((List<Long> ids) -> ids.contains(20L)));
        verify(postSearchRepository).index(argThat((List<PostSearchDocument> docs) -> docs.isEmpty()));
        verify(postIndexDirtyRepository).deleteIfVersionMatches(20L, 1L);
    }

    @Test
    @DisplayName("ES 색인 실패 시 백오프 마킹하고 더티는 남긴다(재처리)")
    void esFailureMarksBackoffAndKeepsDirty() {
        enabledWithBatch();
        given(postIndexDirtyRepository.findProcessable(any(), anyInt())).willReturn(List.of(dirty(30L, 2L)));
        given(postRepository.findByPostIdAndDeletedAtIsNull(30L)).willReturn(Optional.of(publishedPost(30L)));
        given(tagReader.findTags(30L)).willReturn(List.of());
        willThrow(new RuntimeException("ES down")).given(postSearchRepository).index(any());

        postIndexService.syncChangedPosts();

        verify(postIndexDirtyRepository).markFailure(eq(30L), eq(2L), any(LocalDateTime.class));  // 백오프
        verify(postIndexDirtyRepository, never()).deleteIfVersionMatches(anyLong(), anyLong());   // 더티 유지
    }
}
