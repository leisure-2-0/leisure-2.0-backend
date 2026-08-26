package com.leisure.post.dto.result;

import com.leisure.post.domain.PostCategory;

public record PostPinResult(
        Long postId,
        String title,
        PostCategory category,
        
        double latitude,
        double longitude
) {
}
