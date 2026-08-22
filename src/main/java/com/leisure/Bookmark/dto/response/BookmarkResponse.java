package com.leisure.Bookmark.dto.response;

public record BookmarkResponse(
        Long memberId,

        Long postId,

        int bookmarkCount,

        boolean isBookmarked
) {
}
