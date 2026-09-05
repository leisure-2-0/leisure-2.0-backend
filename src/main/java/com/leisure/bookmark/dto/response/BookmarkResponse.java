package com.leisure.bookmark.dto.response;

public record BookmarkResponse(
        Long memberId,

        Long postId,

        int bookmarkCount,

        boolean isBookmarked
) {
}
