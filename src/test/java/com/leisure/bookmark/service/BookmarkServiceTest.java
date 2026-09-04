package com.leisure.bookmark.service;

import com.leisure.bookmark.domain.PostBookmark;
import com.leisure.bookmark.dto.response.BookmarkResponse;
import com.leisure.bookmark.repository.BookmarkRepository;
import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.member.domain.Member;
import com.leisure.member.service.MemberReader;
import com.leisure.post.domain.Post;
import com.leisure.post.domain.PostCategory;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookmarkServiceTest {

    @Mock
    private MemberReader reader;

    @Mock
    private PostRepository postRepository;

    @Mock
    private BookmarkRepository bookmarkRepository;

    @InjectMocks
    private BookmarkService bookmarkService;

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
    @DisplayName("북마크(bookmark)")
    class Bookmark {

        @Test
        @DisplayName("북마크를 저장하고 카운트를 증가시킨 뒤 isBookmarked=true로 응답한다")
        void success() {
            given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member());
            given(postRepository.findByPostIdAndDeletedAtIsNull(POST_ID)).willReturn(Optional.of(publishedPost()));
            given(bookmarkRepository.existsByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(false);
            given(postRepository.findBookmarkCountByPostId(POST_ID)).willReturn(1);

            BookmarkResponse response = bookmarkService.bookmark(PUBLIC_ID, POST_ID);

            assertThat(response.isBookmarked()).isTrue();
            assertThat(response.bookmarkCount()).isEqualTo(1);
            verify(bookmarkRepository).save(any(PostBookmark.class));
            verify(postRepository).increaseBookmarkCount(POST_ID);
        }

        @Test
        @DisplayName("이미 북마크한 글이면 POST_BOOKMARK_ALREADY_BOOKMARKED 예외를 던지고 카운트를 올리지 않는다")
        void alreadyBookmarked() {
            given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member());
            given(postRepository.findByPostIdAndDeletedAtIsNull(POST_ID)).willReturn(Optional.of(publishedPost()));
            given(bookmarkRepository.existsByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(true);

            assertThatThrownBy(() -> bookmarkService.bookmark(PUBLIC_ID, POST_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.POST_BOOKMARK_ALREADY_BOOKMARKED);

            verify(bookmarkRepository, never()).save(any());
            verify(postRepository, never()).increaseBookmarkCount(POST_ID);
        }

        @Test
        @DisplayName("게시되지 않은 글이면 POST_NOT_FOUND 예외를 던진다")
        void notPublished() {
            given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member());
            given(postRepository.findByPostIdAndDeletedAtIsNull(POST_ID)).willReturn(Optional.of(writingPost()));

            assertThatThrownBy(() -> bookmarkService.bookmark(PUBLIC_ID, POST_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.POST_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("북마크 취소(unbookmark)")
    class Unbookmark {

        @Test
        @DisplayName("북마크를 삭제하고 카운트를 감소시킨 뒤 isBookmarked=false로 응답한다")
        void success() {
            given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member());
            given(postRepository.findByPostIdAndDeletedAtIsNull(POST_ID)).willReturn(Optional.of(publishedPost()));
            given(bookmarkRepository.deleteByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(1);
            given(postRepository.findBookmarkCountByPostId(POST_ID)).willReturn(0);

            BookmarkResponse response = bookmarkService.unbookmark(PUBLIC_ID, POST_ID);

            assertThat(response.isBookmarked()).isFalse();
            assertThat(response.bookmarkCount()).isEqualTo(0);
            verify(postRepository).decreaseBookmarkCount(POST_ID);
        }

        @Test
        @DisplayName("북마크하지 않은 글이면 POST_BOOKMARK_NOT_BOOKMARKED_YET 예외를 던지고 카운트를 내리지 않는다")
        void notBookmarkedYet() {
            given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member());
            given(postRepository.findByPostIdAndDeletedAtIsNull(POST_ID)).willReturn(Optional.of(publishedPost()));
            given(bookmarkRepository.deleteByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(0);

            assertThatThrownBy(() -> bookmarkService.unbookmark(PUBLIC_ID, POST_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.POST_BOOKMARK_NOT_BOOKMARKED_YET);

            verify(postRepository, never()).decreaseBookmarkCount(POST_ID);
        }
    }
}
