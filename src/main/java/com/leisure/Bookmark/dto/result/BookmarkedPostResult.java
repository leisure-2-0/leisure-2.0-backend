package com.leisure.Bookmark.dto.result;

import com.leisure.post.domain.PostCategory;

import java.time.LocalDateTime;

public record BookmarkedPostResult(
        Long postId,

        String title,

        PostCategory category,

        int viewCount,

        int likeCount,

        int bookmarkCount,

        boolean isMine,

        boolean isLiked,

        boolean isBookmarked,

        String region,

        LocalDateTime publishedAt,

        LocalDateTime bookmarkedAt,

        AuthorResult author
) {

    public record AuthorResult(
            Long memberId,

            String nickname,

            String profileImageUrl
    ) {}
}
