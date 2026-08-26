package com.leisure.post.dto.response;

import com.leisure.post.domain.PostCategory;
import com.leisure.post.dto.result.PostDetailResult;

import java.time.LocalDateTime;
import java.util.List;

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

    Author author,

    Location location,

    List<String> tags
) {
    public record Author(Long memberId, String nickname, String profileImageUrl) {}
    public record Location(String region, String placeName, String address, Double latitude, Double longitude) {}

    public static PostDetailResponse from(PostDetailResult r, List<String> tags) {
        Author author = new Author(r.author().memberId(), r.author().nickname(), r.author().profileImageUrl());

        Location location = null;

        if (r.location() != null) {
            location = new Location(r.location().region(), r.location().placeName(), r.location().address(),
                    r.location().latitude(), r.location().longitude());
        }

        return new PostDetailResponse(
                r.postId(),
                r.title(),
                r.content(),
                r.category(),
                r.viewCount(),
                r.likeCount(),
                r.bookmarkCount(),
                r.isMine(),
                r.isLiked(),
                r.isBookmarked(),
                r.publishedAt(),
                author,
                location,
                tags
        );
    }
}
