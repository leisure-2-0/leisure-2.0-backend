package com.leisure.Bookmark.repository;

import com.leisure.Bookmark.domain.BookmarkedPostSort;
import com.leisure.Bookmark.dto.response.BookmarkedPostResponse;

import java.util.List;

public interface BookmarkCustom {

    List<BookmarkedPostResponse> findBookmarkedPosts(Long memberId, BookmarkedPostSort sort, long offset, int size);
}
