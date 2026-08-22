package com.leisure.post.dto.response;

import com.leisure.post.domain.PostCategory;

import java.time.LocalDateTime;

public record MyPostResponse(
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

        LocalDateTime createdAt,

        LocalDateTime updatedAt,

        AuthorResponse author
) {

    public record AuthorResponse(
            Long memberId,

            String nickname,

            String profileImageUrl
    ) {
    }
}
