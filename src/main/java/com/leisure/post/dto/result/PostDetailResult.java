package com.leisure.post.dto.result;

import com.leisure.post.domain.PostCategory;
import com.leisure.post.dto.request.LocationRequest;

import java.time.LocalDateTime;

public record PostDetailResult(
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

        AuthorResult author,

        LocationResult location
) {

    public record AuthorResult(
            Long memberId,

            String nickname,

            String profileImageUrl
    ) {}

    public record LocationResult(
            String region,

            String placeName,

            String address,

            Double latitude,

            Double longitude
    ) {}
}
