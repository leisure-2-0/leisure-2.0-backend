package com.leisure.bookmark.dto.response;

import com.leisure.bookmark.dto.result.BookmarkedPostResult;
import com.leisure.post.domain.PostCategory;

import java.time.LocalDateTime;
import java.util.List;

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

        String region,

        LocalDateTime publishedAt,

        LocalDateTime bookmarkedAt,

        AuthorResponse author,

        List<String> tags
) {

    public record AuthorResponse(
            Long memberId,

            String nickname,

            String profileImageUrl
    ) {
    }

    public static BookmarkedPostResponse from(BookmarkedPostResult r, List<String> tags) {
        AuthorResponse author = new AuthorResponse(
                r.author().memberId(), r.author().nickname(), r.author().profileImageUrl());

        return new BookmarkedPostResponse(
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
                r.bookmarkedAt(),
                author,
                tags
        );
    }
}
