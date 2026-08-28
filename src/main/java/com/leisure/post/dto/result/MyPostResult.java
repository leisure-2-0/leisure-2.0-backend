package com.leisure.post.dto.result;

import com.leisure.post.domain.PostCategory;

import java.time.LocalDateTime;

public record MyPostResult(
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

        LocalDateTime createdAt,

        LocalDateTime updatedAt,

        AuthorResult author
) {

    public record AuthorResult(
            Long memberId,

            String nickname,

            String profileImageUrl
    ) {}
}
