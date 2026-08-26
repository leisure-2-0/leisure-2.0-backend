package com.leisure.post.dto.result;

import com.leisure.post.domain.PostCategory;

import java.time.LocalDateTime;

public record DraftDetailResult(
        Long postId,

        String title,

        String content,

        PostCategory category,

        LocalDateTime updatedAt,

        LocationResult location
) {

    public record LocationResult(
            String region,

            String placeName,

            String address,

            Double latitude,

            Double longitude
    ) {}
}
