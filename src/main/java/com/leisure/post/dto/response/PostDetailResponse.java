package com.leisure.post.dto.response;

import com.leisure.post.domain.PostCategory;

import java.time.LocalDateTime;

public record PostDetailResponse(
        Long postId,

        String title,

        String content,

        PostCategory category,

        int viewCount,

        int likeCount,

        int bookmarkCount,

        boolean isMine,

        boolean isLiked,

        boolean isBookmarked,

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
