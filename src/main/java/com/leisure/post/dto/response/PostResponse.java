package com.leisure.post.dto.response;

import com.leisure.post.domain.PostCategory;
import com.leisure.post.dto.result.PostResult;

import java.time.LocalDateTime;
import java.util.List;

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

        AuthorResponse author,

        List<String> tags
) {

    public record AuthorResponse(
            Long memberId,

            String nickname,

            String profileImageUrl
    ) {
    }

    public static PostResponse from(PostResult r, List<String> tags) {
        AuthorResponse author = new AuthorResponse(
                r.author().memberId(), r.author().nickname(), r.author().profileImageUrl());

        return new PostResponse(
                r.postId(),
                r.title(),
                r.category(),
                r.viewCount(),
                r.likeCount(),
                r.bookmarkCount(),
                r.isLiked(),
                r.isBookmarked(),
                r.region(),
                r.publishedAt(),
                author,
                tags
        );
    }
}
