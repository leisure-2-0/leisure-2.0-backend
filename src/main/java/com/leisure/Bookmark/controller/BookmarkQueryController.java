package com.leisure.Bookmark.controller;

import com.leisure.Bookmark.domain.BookmarkedPostSort;
import com.leisure.Bookmark.dto.response.BookmarkedPostListResponse;
import com.leisure.Bookmark.service.BookmarkQueryService;
import com.leisure.global.auth.CurrentMember;
import com.leisure.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BookmarkQueryController {

    private final BookmarkQueryService service;

    @GetMapping("/members/me/bookmarks")
    public ResponseEntity<ApiResponse<BookmarkedPostListResponse>> getBookmarkedPosts(
            @CurrentMember String publicId,
            @RequestParam(defaultValue = "LATEST") BookmarkedPostSort sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {

        BookmarkedPostListResponse response = service.getBookmarkedPosts(publicId, sort, page, size);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("북마크한 게시글 목록 조회에 성공했습니다.", response));
    }
}
