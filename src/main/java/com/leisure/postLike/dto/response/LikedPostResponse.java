package com.leisure.postLike.dto.response;

import com.leisure.post.domain.PostCategory;
import com.leisure.postLike.dto.result.LikedPostResult;

import java.time.LocalDateTime;
import java.util.List;

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

        AuthorResponse author,

        List<String> tags
) {

    public record AuthorResponse(
            Long memberId,

            String nickname,

            String profileImageUrl
    ) {
    }

    public static LikedPostResponse from(LikedPostResult r, List<String> tags) {
        AuthorResponse author = new AuthorResponse(
                r.author().memberId(), r.author().nickname(), r.author().profileImageUrl());

        return new LikedPostResponse(
                r.postId(),
                r.title(),
                r.category(),
                r.viewCount(),
                r.likeCount(),
                r.bookmarkCount(),
                r.isMine(),
                r.isLiked(),
                r.isBookmarked(),
                r.region(),
                r.publishedAt(),
                r.likedAt(),
                author,
                tags
        );
    }
}
