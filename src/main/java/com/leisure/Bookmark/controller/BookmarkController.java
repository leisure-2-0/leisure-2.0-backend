package com.leisure.Bookmark.controller;

import com.leisure.global.auth.CurrentMember;
import com.leisure.global.response.ApiResponse;
import com.leisure.Bookmark.dto.response.BookmarkResponse;
import com.leisure.Bookmark.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService service;

    @PostMapping("/posts/{postId}/bookmarks")
    public ResponseEntity<ApiResponse<BookmarkResponse>> bookmark(
            @CurrentMember String publicId,
            @PathVariable Long postId
    ) {
        BookmarkResponse response = service.bookmark(publicId, postId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response.postId() + "번 게시글을 북마크했습니다.", response));
    }

    @DeleteMapping("/posts/{postId}/bookmarks")
    public ResponseEntity<ApiResponse<BookmarkResponse>> unbookmark(
            @CurrentMember String publicId,
            @PathVariable Long postId
    ) {
        BookmarkResponse response = service.unbookmark(publicId, postId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response.postId() + "번 게시글 북마크를 취소했습니다.", response));
    }
}
