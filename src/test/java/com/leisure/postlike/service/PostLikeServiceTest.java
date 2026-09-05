package com.leisure.postlike.service;

import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.member.domain.Member;
import com.leisure.member.service.MemberReader;
import com.leisure.post.domain.Post;
import com.leisure.post.domain.PostCategory;
import com.leisure.post.repository.PostRepository;
import com.leisure.postlike.domain.PostLike;
import com.leisure.postlike.dto.response.PostLikeResponse;
import com.leisure.postlike.repository.PostLikeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostLikeServiceTest {

    @Mock
    private MemberReader reader;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostLikeRepository likeRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PostLikeService postLikeService;

    private static final String PUBLIC_ID = "public-id";
    private static final Long MEMBER_ID = 1L;
    private static final Long POST_ID = 10L;

    private Member member() {
        Member member = Member.create("user@leisure.com", "ENCODED", "nick");
        ReflectionTestUtils.setField(member, "memberId", MEMBER_ID);
        return member;
    }

    private Post publishedPost() {
        Post post = Post.startWriting(MEMBER_ID);
        ReflectionTestUtils.setField(post, "postId", POST_ID);
        post.applyContent("제목", "본문", PostCategory.RESTAURANT, null);
        post.publish();
        return post;
    }

    private Post writingPost() {
        Post post = Post.startWriting(MEMBER_ID);
        ReflectionTestUtils.setField(post, "postId", POST_ID);
        return post;
    }

    @Nested
    @DisplayName("좋아요(like)")
    class Like {

        @Test
        @DisplayName("좋아요를 저장하고 카운트를 증가시킨 뒤 isLiked=true로 응답한다")
        void success() {
            // given
            given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member());
            given(postRepository.findByPostIdAndDeletedAtIsNull(POST_ID)).willReturn(Optional.of(publishedPost()));
            given(likeRepository.existsByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(false);
            given(postRepository.findLikeCountByPostId(POST_ID)).willReturn(1);

            // when
            PostLikeResponse response = postLikeService.like(PUBLIC_ID, POST_ID);

            // then
            assertThat(response.isLiked()).isTrue();
            assertThat(response.likeCount()).isEqualTo(1);
            verify(likeRepository).save(any(PostLike.class));
            verify(postRepository).increaseLikeCount(POST_ID);
        }

        @Test
        @DisplayName("이미 좋아요한 글이면 POST_LIKE_ALREADY_LIKED 예외를 던지고 카운트를 올리지 않는다")
        void alreadyLiked() {
            given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member());
            given(postRepository.findByPostIdAndDeletedAtIsNull(POST_ID)).willReturn(Optional.of(publishedPost()));
            given(likeRepository.existsByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(true);

            assertThatThrownBy(() -> postLikeService.like(PUBLIC_ID, POST_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.POST_LIKE_ALREADY_LIKED);

            verify(likeRepository, never()).save(any());
            verify(postRepository, never()).increaseLikeCount(POST_ID);
        }

        @Test
        @DisplayName("게시되지 않은 글이면 POST_NOT_FOUND 예외를 던진다")
        void notPublished() {
            given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member());
            given(postRepository.findByPostIdAndDeletedAtIsNull(POST_ID)).willReturn(Optional.of(writingPost()));

            assertThatThrownBy(() -> postLikeService.like(PUBLIC_ID, POST_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.POST_NOT_FOUND);
        }

        @Test
        @DisplayName("존재하지 않는 글이면 POST_NOT_FOUND 예외를 던진다")
        void notFound() {
            given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member());
            given(postRepository.findByPostIdAndDeletedAtIsNull(POST_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> postLikeService.like(PUBLIC_ID, POST_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.POST_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("좋아요 취소(unlike)")
    class Unlike {

        @Test
        @DisplayName("좋아요를 삭제하고 카운트를 감소시킨 뒤 isLiked=false로 응답한다")
        void success() {
            given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member());
            given(postRepository.findByPostIdAndDeletedAtIsNull(POST_ID)).willReturn(Optional.of(publishedPost()));
            given(likeRepository.deleteByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(1);
            given(postRepository.findLikeCountByPostId(POST_ID)).willReturn(0);

            PostLikeResponse response = postLikeService.unlike(PUBLIC_ID, POST_ID);

            assertThat(response.isLiked()).isFalse();
            assertThat(response.likeCount()).isEqualTo(0);
            verify(postRepository).decreaseLikeCount(POST_ID);
        }

        @Test
        @DisplayName("좋아요하지 않은 글이면 POST_LIKE_NOT_LIKED_YET 예외를 던지고 카운트를 내리지 않는다")
        void notLikedYet() {
            given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member());
            given(postRepository.findByPostIdAndDeletedAtIsNull(POST_ID)).willReturn(Optional.of(publishedPost()));
            given(likeRepository.deleteByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(0);

            assertThatThrownBy(() -> postLikeService.unlike(PUBLIC_ID, POST_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.POST_LIKE_NOT_LIKED_YET);

            verify(postRepository, never()).decreaseLikeCount(POST_ID);
        }
    }
}
