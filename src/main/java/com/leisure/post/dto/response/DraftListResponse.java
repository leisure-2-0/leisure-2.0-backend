package com.leisure.post.dto.response;

import com.leisure.post.domain.PostCategory;

import java.time.LocalDateTime;

public record DraftListResponse(
        Long postId,

        String title,

        PostCategory category,

        LocalDateTime updatedAt
) {}
