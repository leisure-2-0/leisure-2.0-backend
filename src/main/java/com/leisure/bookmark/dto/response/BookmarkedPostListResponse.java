package com.leisure.bookmark.dto.response;

import java.util.List;

public record BookmarkedPostListResponse(

        int page,

        int size,

        long totalElements,

        int totalPages,

        boolean hasNext,

        List<BookmarkedPostResponse> content
) {
}
