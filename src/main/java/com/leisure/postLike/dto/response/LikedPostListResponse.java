package com.leisure.postLike.dto.response;

import java.util.List;

public record LikedPostListResponse(
        List<LikedPostResponse> content,

        int page,

        int size,

        long totalElements,

        int totalPages,

        boolean hasNext
) {}
