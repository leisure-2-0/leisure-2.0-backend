package com.leisure.bookmark.controller;

import com.leisure.bookmark.domain.BookmarkedPostSort;
import com.leisure.bookmark.dto.response.BookmarkedPostListResponse;
import com.leisure.bookmark.service.BookmarkQueryService;
import com.leisure.global.auth.CurrentMember;
import com.leisure.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "북마크 조회(Bookmark Query)",
        description = "내가 북마크한 게시글 목록 조회"
)
@RestController
@RequiredArgsConstructor
public class BookmarkQueryController {

    private final BookmarkQueryService service;

    @Operation(summary = "내 북마크 목록 조회", description = "본인이 북마크한 게시글을 오프셋 기반으로 조회한다.")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/members/me/bookmarks")
    public ResponseEntity<ApiResponse<BookmarkedPostListResponse>> getBookmarkedPosts(
            @CurrentMember String publicId,
            @Parameter(description = "정렬 기준. LATEST=최신순, POPULAR=인기순")
            @RequestParam(defaultValue = "LATEST") BookmarkedPostSort sort,
            @Parameter(description = "페이지 번호(0부터 시작, 기본 0)")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기(1~30, 기본 10)")
            @RequestParam(required = false) Integer size
    ) {

        BookmarkedPostListResponse response = service.getBookmarkedPosts(publicId, sort, page, size);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("북마크한 게시글 목록 조회에 성공했습니다.", response));
    }
}
