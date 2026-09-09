package com.leisure.search.dto.response;

import com.leisure.post.dto.response.PostResponse;

import java.util.List;

public record PostSearchResponse(
        List<PostResponse> content,

        int page,

        int size,

        long totalElements,

        int totalPages,

        boolean hasNext
) {
}
