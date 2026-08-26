package com.leisure.Bookmark.repository;

import com.leisure.Bookmark.domain.BookmarkedPostSort;
import com.leisure.Bookmark.dto.result.BookmarkedPostResult;

import java.util.List;

public interface BookmarkCustom {

    List<BookmarkedPostResult> findBookmarkedPosts(Long memberId, BookmarkedPostSort sort, long offset, int size);
}
