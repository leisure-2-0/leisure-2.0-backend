package com.leisure.post.domain;

import java.time.LocalDateTime;

public record PostCursor(
        Long postId,

        LocalDateTime publishedAt,

        Integer likeCount
) {
}
