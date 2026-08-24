package com.leisure.post.dto.response;

import com.leisure.post.domain.PostCategory;

import java.time.LocalDateTime;

public record PostResponse(
        Long postId,

        String title,

        PostCategory category,

        int viewCount,

        int likeCount,

        int bookmarkCount,

        boolean isLiked,

        boolean isBookmarked,

        String region,

        LocalDateTime publishedAt,

        AuthorResponse author
) {

    public record AuthorResponse(
            Long memberId,

            String nickname,

            String profileImageUrl
    ) {
    }
}
