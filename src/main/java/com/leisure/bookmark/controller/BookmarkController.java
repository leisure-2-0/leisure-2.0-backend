package com.leisure.bookmark.controller;

import com.leisure.global.auth.CurrentMember;
import com.leisure.global.response.ApiResponse;
import com.leisure.bookmark.dto.response.BookmarkResponse;
import com.leisure.bookmark.service.BookmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "북마크(Bookmark)",
        description = "게시글 북마크 등록/취소"
)
@RestController
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService service;

    @Operation(summary = "북마크 등록", description = "게시글을 북마크하고 최신 북마크 수를 반환한다. 이미 했으면 409.")
    @SecurityRequirement(name = "BearerAuth")
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

    @Operation(summary = "북마크 취소", description = "게시글 북마크를 취소하고 최신 북마크 수를 반환한다. 안 했으면 404.")
    @SecurityRequirement(name = "BearerAuth")
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
