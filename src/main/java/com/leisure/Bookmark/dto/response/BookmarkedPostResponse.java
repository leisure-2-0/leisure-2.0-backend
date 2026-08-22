package com.leisure.Bookmark.dto.response;

import com.leisure.post.domain.PostCategory;

import java.time.LocalDateTime;

public record BookmarkedPostResponse(
        Long postId,

        String title,

        PostCategory category,

        int viewCount,

        int likeCount,

        int bookmarkCount,

        boolean isMine,

        boolean isLiked,

        boolean isBookmarked,

        LocalDateTime publishedAt,

        LocalDateTime bookmarkedAt,

        AuthorResponse author
) {

    public record AuthorResponse(
            Long memberId,

            String nickname,

            String profileImageUrl
    ) {
    }
}
