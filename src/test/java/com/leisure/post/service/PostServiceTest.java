package com.leisure.post.service;

import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.member.domain.Member;
import com.leisure.member.service.MemberReader;
import com.leisure.post.domain.Post;
import com.leisure.post.domain.PostCategory;
import com.leisure.post.domain.PostStatus;
import com.leisure.post.dto.request.PostPublishRequest;
import com.leisure.post.dto.request.PostSaveRequest;
import com.leisure.post.dto.response.PostPublishResponse;
import com.leisure.post.dto.response.PostSaveResponse;
import com.leisure.post.dto.response.PostStartResponse;
import com.leisure.post.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository repository;

    @Mock
    private MemberReader reader;

    @InjectMocks
    private PostService postService;

    private static final String PUBLIC_ID = "public-id";
    private static final Long MEMBER_ID = 1L;
    private static final Long OTHER_MEMBER_ID = 2L;
    private static final Long POST_ID = 10L;

    private Member member(Long memberId) {
        Member member = Member.create("user@leisure.com", "ENCODED", "nick", null);
        ReflectionTestUtils.setField(member, "memberId", memberId);
        return member;
    }

    private Post writingPost(Long memberId) {
        Post post = Post.startWriting(memberId);
        ReflectionTestUtils.setField(post, "postId", POST_ID);
        return post;
    }

    private Post publishedPost(Long memberId) {
        Post post = writingPost(memberId);
        post.applyContent("제목", "본문", PostCategory.RESTAURANT);
        post.publish();
        return post;
    }

    @Test
    @DisplayName("작성 시작 시 WRITING 상태의 빈 글을 생성하고 발급된 postId를 반환한다")
    void startPosting_success() {
        // given
        given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member(MEMBER_ID));
        given(repository.save(any(Post.class))).willAnswer(invocation -> {
            Post saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "postId", POST_ID);
            return saved;
        });

        // when
        PostStartResponse response = postService.startPosting(PUBLIC_ID);

        // then
        assertThat(response.postId()).isEqualTo(POST_ID);
        assertThat(response.status()).isEqualTo(PostStatus.WRITING);
        verify(repository).save(any(Post.class));
    }

    @Nested
    @DisplayName("임시 저장(saveDraft)")
    class SaveDraft {

        @Test
        @DisplayName("내용을 반영하고 WRITING이면 DRAFT로 승격한다")
        void success() {
            // given
            Post post = writingPost(MEMBER_ID);
            given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member(MEMBER_ID));
            given(repository.findById(POST_ID)).willReturn(Optional.of(post));
            PostSaveRequest request = new PostSaveRequest("제목", "본문", PostCategory.HOTEL);

            // when
            PostSaveResponse response = postService.saveDraft(PUBLIC_ID, POST_ID, request);

            // then
            assertThat(response.status()).isEqualTo(PostStatus.DRAFT);
            assertThat(post.getTitle()).isEqualTo("제목");
            assertThat(post.getCategory()).isEqualTo(PostCategory.HOTEL);
        }

        @Test
        @DisplayName("존재하지 않는 글이면 POST_NOT_FOUND 예외를 던진다")
        void notFound() {
            given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member(MEMBER_ID));
            given(repository.findById(POST_ID)).willReturn(Optional.empty());
            PostSaveRequest request = new PostSaveRequest("제목", null, null);

            assertThatThrownBy(() -> postService.saveDraft(PUBLIC_ID, POST_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.POST_NOT_FOUND);
        }

        @Test
        @DisplayName("작성자가 아니면 POST_FORBIDDEN 예외를 던진다")
        void forbidden() {
            Post post = writingPost(OTHER_MEMBER_ID);
            given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member(MEMBER_ID));
            given(repository.findById(POST_ID)).willReturn(Optional.of(post));
            PostSaveRequest request = new PostSaveRequest("제목", null, null);

            assertThatThrownBy(() -> postService.saveDraft(PUBLIC_ID, POST_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.POST_FORBIDDEN);
        }

        @Test
        @DisplayName("이미 게시된(PUBLISHED) 글은 수정할 수 없어 POST_NOT_EDITABLE 예외를 던진다")
        void notEditable() {
            Post post = publishedPost(MEMBER_ID);
            given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member(MEMBER_ID));
            given(repository.findById(POST_ID)).willReturn(Optional.of(post));
            PostSaveRequest request = new PostSaveRequest("바꾼제목", null, null);

            assertThatThrownBy(() -> postService.saveDraft(PUBLIC_ID, POST_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.POST_NOT_EDITABLE);
        }
    }

    @Nested
    @DisplayName("게시(publish)")
    class Publish {

        @Test
        @DisplayName("요청 내용을 반영하고 WRITING에서 바로 PUBLISHED로 전이하며 게시 시각을 찍는다")
        void success() {
            // given
            Post post = writingPost(MEMBER_ID);
            given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member(MEMBER_ID));
            given(repository.findById(POST_ID)).willReturn(Optional.of(post));
            PostPublishRequest request = new PostPublishRequest("제목", "본문", PostCategory.RESTAURANT);

            // when
            PostPublishResponse response = postService.publish(PUBLIC_ID, POST_ID, request);

            // then
            assertThat(response.status()).isEqualTo(PostStatus.PUBLISHED);
            assertThat(post.getPublishedAt()).isNotNull();
        }

        @Test
        @DisplayName("제목이 없으면 POST_TITLE_REQUIRED 예외를 던진다")
        void titleRequired() {
            Post post = writingPost(MEMBER_ID);
            given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member(MEMBER_ID));
            given(repository.findById(POST_ID)).willReturn(Optional.of(post));
            PostPublishRequest request = new PostPublishRequest(null, "본문", PostCategory.RESTAURANT);

            assertThatThrownBy(() -> postService.publish(PUBLIC_ID, POST_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.POST_TITLE_REQUIRED);
        }
    }
}
