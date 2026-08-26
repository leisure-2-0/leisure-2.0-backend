package com.leisure.postLike.controller;

import com.leisure.global.auth.CurrentMember;
import com.leisure.global.response.ApiResponse;
import com.leisure.postLike.domain.LikedPostSort;
import com.leisure.postLike.dto.response.LikedPostListResponse;
import com.leisure.postLike.service.PostLikeQueryService;
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
        name = "좋아요 조회(Like Query)",
        description = "내가 좋아요한 게시글 목록 조회"
)
@RestController
@RequiredArgsConstructor
public class PostLikeQueryController {

    private final PostLikeQueryService service;

    @Operation(summary = "내 좋아요 목록 조회", description = "본인이 좋아요한 게시글을 오프셋 기반으로 조회한다.")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/members/me/likes")
    public ResponseEntity<ApiResponse<LikedPostListResponse>> getLikedPosts(
            @CurrentMember String publicId,
            @Parameter(description = "정렬 기준. LATEST=최신순, POPULAR=인기순")
            @RequestParam(defaultValue = "LATEST")LikedPostSort sort,
            @Parameter(description = "페이지 번호(0부터 시작, 기본 0)")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기(1~30, 기본 10)")
            @RequestParam(required = false) Integer size
    ) {

        LikedPostListResponse response = service.getLikedPosts(publicId, sort, page, size);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("좋아요한 게시글 목록 조회에 성공했습니다.", response));
    }
}
