package com.leisure.bookmark.repository;

import com.leisure.bookmark.domain.BookmarkedPostSort;
import com.leisure.bookmark.dto.result.BookmarkedPostResult;

import java.util.List;

public interface BookmarkRepositoryCustom {

    List<BookmarkedPostResult> findBookmarkedPosts(Long memberId, BookmarkedPostSort sort, long offset, int size);
}
