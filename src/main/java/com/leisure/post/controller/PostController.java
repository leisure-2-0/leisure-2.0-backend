package com.leisure.post.controller;

import com.leisure.global.auth.CurrentMember;
import com.leisure.global.response.ApiResponse;
import com.leisure.post.dto.request.PostEditRequest;
import com.leisure.post.dto.request.PostPublishRequest;
import com.leisure.post.dto.request.PostSaveRequest;
import com.leisure.post.dto.response.*;
import com.leisure.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService service;

    @PostMapping("/posts")
    public ResponseEntity<ApiResponse<PostStartResponse>> startPosting(@CurrentMember String publicId) {

        PostStartResponse response = service.startPosting(publicId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("게시글 작성을 시작했습니다.", response));
    }

    @PatchMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<PostSaveResponse>> saveDraft(@CurrentMember String publicId, @PathVariable Long postId, @Valid @RequestBody PostSaveRequest request) {

        PostSaveResponse response = service.saveDraft(publicId, postId, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("게시글이 임시 저장되었습니다.", response));
    }

    @PatchMapping("/posts/{postId}/publish")
    public ResponseEntity<ApiResponse<PostPublishResponse>> publish(@CurrentMember String publicId, @PathVariable Long postId, @Valid @RequestBody PostPublishRequest request) {

        PostPublishResponse response = service.publish(publicId, postId, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("게시글이 게시되었습니다", response));
    }

    @PatchMapping("/posts/{postId}/content")
    public ResponseEntity<ApiResponse<PostEditResponse>> editPost(@CurrentMember String publicId, @PathVariable Long postId, @Valid @RequestBody PostEditRequest request) {

        PostEditResponse response = service.editPost(publicId, postId, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("게시글이 수정되었습니다.", response));
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<PostDeleteResponse>> deletePost(@CurrentMember String publicId, @PathVariable Long postId) {

        PostDeleteResponse response = service.deletePost(publicId, postId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("게시글이 삭제되었습니다.", response));
    }
}
