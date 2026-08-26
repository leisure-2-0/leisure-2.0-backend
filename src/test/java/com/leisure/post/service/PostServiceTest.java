package com.leisure.post.service;

import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.member.domain.Member;
import com.leisure.member.service.MemberReader;
import com.leisure.post.domain.Post;
import com.leisure.post.domain.PostCategory;
import com.leisure.post.domain.PostStatus;
import com.leisure.post.dto.request.PostEditRequest;
import com.leisure.post.dto.request.PostPublishRequest;
import com.leisure.post.dto.request.PostSaveRequest;
import com.leisure.post.dto.response.PostDeleteResponse;
import com.leisure.post.dto.response.PostEditResponse;
import com.leisure.post.dto.response.PostPublishResponse;
import com.leisure.post.dto.response.PostSaveResponse;
import com.leisure.post.dto.response.PostStartResponse;
import com.leisure.post.repository.PostRepository;
import com.leisure.tag.repository.TagRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository repository;

    @Mock
    private MemberReader reader;

    @Mock
    private TagRepository tagRepository;

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
        post.applyContent("제목", "본문", PostCategory.RESTAURANT, null);
        post.publish();
        return post;
    }

    private Post draftPost(Long memberId) {
        Post post = writingPost(memberId);
        post.markAsDraft();
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
            given(repository.findByPostIdAndDeletedAtIsNull(POST_ID)).willReturn(Optional.of(post));
            PostSaveRequest request = new PostSaveRequest("제목", "본문", PostCategory.HOTEL, null, null);

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
            given(repository.findByPostIdAndDeletedAtIsNull(POST_ID)).willReturn(Optional.empty());
            PostSaveRequest request = new PostSaveRequest("제목", null, null, null, null);

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
            given(repository.findByPostIdAndDeletedAtIsNull(POST_ID)).willReturn(Optional.of(post));
            PostSaveRequest request = new PostSaveRequest("제목", null, null, null, null);

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
            given(repository.findByPostIdAndDeletedAtIsNull(POST_ID)).willReturn(Optional.of(post));
            PostSaveRequest request = new PostSaveRequest("바꾼제목", null, null, null, null);

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
            given(repository.findByPostIdAndDeletedAtIsNull(POST_ID)).willReturn(Optional.of(post));
            PostPublishRequest request = new PostPublishRequest("제목", "본문", PostCategory.RESTAURANT, null, null);

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
            given(repository.findByPostIdAndDeletedAtIsNull(POST_ID)).willReturn(Optional.of(post));
            PostPublishRequest request = new PostPublishRequest(null, "본문", PostCategory.RESTAURANT, null, null);

            assertThatThrownBy(() -> postService.publish(PUBLIC_ID, POST_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.POST_TITLE_REQUIRED);
        }
    }

    @Nested
    @DisplayName("게시글 수정(editPost)")
    class EditPost {

        @Test
        @DisplayName("게시된 본인 글이면 내용을 수정하고 postId를 반환한다")
        void success() {
            // given
            Post post = publishedPost(MEMBER_ID);
            given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member(MEMBER_ID));
            given(repository.findByPostIdAndDeletedAtIsNull(POST_ID)).willReturn(Optional.of(post));
            PostEditRequest request = new PostEditRequest("수정 제목", "수정 본문", PostCategory.HOTEL, null, null);

            // when
            PostEditResponse response = postService.editPost(PUBLIC_ID, POST_ID, request);

            // then
            assertThat(response.postId()).isEqualTo(POST_ID);
            assertThat(post.getTitle()).isEqualTo("수정 제목");
            assertThat(post.getContent()).isEqualTo("수정 본문");
            assertThat(post.getCategory()).isEqualTo(PostCategory.HOTEL);
            assertThat(post.getStatus()).isEqualTo(PostStatus.PUBLISHED);
        }

        @Test
        @DisplayName("게시되지 않은 글이면 POST_NOT_EDITABLE 예외를 던진다")
        void notPublished() {
            Post post = writingPost(MEMBER_ID);
            given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member(MEMBER_ID));
            given(repository.findByPostIdAndDeletedAtIsNull(POST_ID)).willReturn(Optional.of(post));
            PostEditRequest request = new PostEditRequest("수정 제목", null, null, null, null);

            assertThatThrownBy(() -> postService.editPost(PUBLIC_ID, POST_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.POST_NOT_EDITABLE);
        }

        @Test
        @DisplayName("제목이 공백이면 POST_TITLE_REQUIRED 예외를 던진다")
        void blankTitle() {
            Post post = publishedPost(MEMBER_ID);
            given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member(MEMBER_ID));
            given(repository.findByPostIdAndDeletedAtIsNull(POST_ID)).willReturn(Optional.of(post));
            PostEditRequest request = new PostEditRequest("   ", null, null, null, null);

            assertThatThrownBy(() -> postService.editPost(PUBLIC_ID, POST_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.POST_TITLE_REQUIRED);
        }

        @Test
        @DisplayName("작성자가 아니면 POST_FORBIDDEN 예외를 던진다")
        void forbidden() {
            Post post = publishedPost(OTHER_MEMBER_ID);
            given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member(MEMBER_ID));
            given(repository.findByPostIdAndDeletedAtIsNull(POST_ID)).willReturn(Optional.of(post));
            PostEditRequest request = new PostEditRequest("수정 제목", null, null, null, null);

            assertThatThrownBy(() -> postService.editPost(PUBLIC_ID, POST_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.POST_FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("게시글 삭제(deletePost)")
    class DeletePost {

        @Test
        @DisplayName("게시글(PUBLISHED)이면 소프트 삭제하고 하드 삭제/태그 정리는 하지 않는다")
        void published_softDelete() {
            // given
            Post post = publishedPost(MEMBER_ID);
            given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member(MEMBER_ID));
            given(repository.findByPostIdAndDeletedAtIsNull(POST_ID)).willReturn(Optional.of(post));

            // when
            PostDeleteResponse response = postService.deletePost(PUBLIC_ID, POST_ID);

            // then — 소프트 삭제(deleted_at 기록), 하드 삭제/태그 삭제 없음
            assertThat(response.postId()).isEqualTo(POST_ID);
            assertThat(post.getDeletedAt()).isNotNull();
            verify(repository, never()).delete(post);
            verify(tagRepository, never()).deleteByPostId(POST_ID);
        }

        @Test
        @DisplayName("초안(DRAFT)이면 태그를 정리하고 하드 삭제하며 소프트 삭제(deleted_at)는 하지 않는다")
        void draft_hardDelete() {
            // given
            Post post = draftPost(MEMBER_ID);
            given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member(MEMBER_ID));
            given(repository.findByPostIdAndDeletedAtIsNull(POST_ID)).willReturn(Optional.of(post));

            // when
            PostDeleteResponse response = postService.deletePost(PUBLIC_ID, POST_ID);

            // then — 태그 정리 + 하드 삭제, 소프트 삭제 아님(deleted_at null)
            assertThat(response.postId()).isEqualTo(POST_ID);
            assertThat(post.getDeletedAt()).isNull();
            verify(tagRepository).deleteByPostId(POST_ID);
            verify(repository).delete(post);
        }

        @Test
        @DisplayName("존재하지 않는 글이면 POST_NOT_FOUND 예외를 던진다")
        void notFound() {
            given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member(MEMBER_ID));
            given(repository.findByPostIdAndDeletedAtIsNull(POST_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> postService.deletePost(PUBLIC_ID, POST_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.POST_NOT_FOUND);
        }

        @Test
        @DisplayName("작성자가 아니면 POST_FORBIDDEN 예외를 던진다")
        void forbidden() {
            Post post = publishedPost(OTHER_MEMBER_ID);
            given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member(MEMBER_ID));
            given(repository.findByPostIdAndDeletedAtIsNull(POST_ID)).willReturn(Optional.of(post));

            assertThatThrownBy(() -> postService.deletePost(PUBLIC_ID, POST_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.POST_FORBIDDEN);
        }
    }
}
