package com.leisure.post.dto.response;

import com.leisure.post.domain.PostCategory;
import com.leisure.post.dto.result.MyPostResult;

import java.time.LocalDateTime;
import java.util.List;

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

        String region,

        LocalDateTime publishedAt,

        LocalDateTime createdAt,

        LocalDateTime updatedAt,

        AuthorResponse author,

        List<String> tags
) {

    public record AuthorResponse(
            Long memberId,

            String nickname,

            String profileImageUrl
    ) {
    }

    public static MyPostResponse from(MyPostResult r, List<String> tags) {
        AuthorResponse author = new AuthorResponse(
                r.author().memberId(), r.author().nickname(), r.author().profileImageUrl());

        return new MyPostResponse(
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
                r.createdAt(),
                r.updatedAt(),
                author,
                tags
        );
    }
}
