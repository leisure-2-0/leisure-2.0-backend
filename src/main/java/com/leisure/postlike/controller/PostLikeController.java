package com.leisure.postlike.controller;

import com.leisure.global.auth.CurrentMember;
import com.leisure.global.response.ApiResponse;
import com.leisure.postlike.dto.response.PostLikeResponse;
import com.leisure.postlike.service.PostLikeService;
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
        name = "좋아요(Like)",
        description = "게시글 좋아요 등록/취소"
)
@RestController
@RequiredArgsConstructor
public class PostLikeController {

    private final PostLikeService service;

    @Operation(summary = "좋아요 등록", description = "게시글에 좋아요를 누르고 최신 좋아요 수를 반환한다. 이미 눌렀으면 409.")
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/posts/{postId}/likes")
    public ResponseEntity<ApiResponse<PostLikeResponse>> like(
            @CurrentMember String publicId,
            @PathVariable Long postId
    ) {
        PostLikeResponse response = service.like(publicId, postId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response.postId() + "번 게시글 좋아요를 눌렀습니다.", response));
    }

    @Operation(summary = "좋아요 취소", description = "게시글 좋아요를 취소하고 최신 좋아요 수를 반환한다. 누르지 않았으면 404.")
    @SecurityRequirement(name = "BearerAuth")
    @DeleteMapping("/posts/{postId}/likes")
    public ResponseEntity<ApiResponse<PostLikeResponse>> unlike(
            @CurrentMember String publicId,
            @PathVariable Long postId
    ) {
        PostLikeResponse response = service.unlike(publicId, postId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response.postId() + "번 게시글 좋아요를 취소했습니다.", response));
    }
}
