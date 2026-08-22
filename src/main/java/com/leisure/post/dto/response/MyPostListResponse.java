package com.leisure.post.dto.response;

import java.util.List;

public record MyPostListResponse(
        List<MyPostResponse> content,

        int page,

        int size,

        long totalElements,

        int totalPages,

        boolean hasNext
) {
}
