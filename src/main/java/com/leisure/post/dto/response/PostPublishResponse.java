package com.leisure.post.dto.response;

import com.leisure.post.domain.PostStatus;

import java.time.LocalDateTime;

public record PostPublishResponse(
        Long postId,

        PostStatus status,

        LocalDateTime publishAt
) {}
