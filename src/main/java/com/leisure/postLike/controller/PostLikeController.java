package com.leisure.postLike.controller;

import com.leisure.global.auth.CurrentMember;
import com.leisure.global.response.ApiResponse;
import com.leisure.postLike.dto.response.PostLikeResponse;
import com.leisure.postLike.service.PostLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PostLikeController {

    private final PostLikeService service;

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
