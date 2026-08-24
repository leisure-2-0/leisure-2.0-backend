package com.leisure.postLike.dto.response;

import com.leisure.post.domain.PostCategory;

import java.time.LocalDateTime;

public record LikedPostResponse(
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

        LocalDateTime likedAt,

        AuthorResponse author
) {

    public record AuthorResponse(
            Long memberId,

            String nickname,

            String profileImageUrl
    ) {
    }
}
