package com.leisure.Bookmark.repository;

import com.leisure.Bookmark.domain.BookmarkedPostSort;
import com.leisure.Bookmark.dto.result.BookmarkedPostResult;

import java.util.List;

public interface BookmarkRepositoryCustom {

    List<BookmarkedPostResult> findBookmarkedPosts(Long memberId, BookmarkedPostSort sort, long offset, int size);
}
