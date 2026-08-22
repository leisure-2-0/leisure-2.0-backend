package com.leisure.postLike.controller;

import com.leisure.global.auth.CurrentMember;
import com.leisure.global.response.ApiResponse;
import com.leisure.postLike.domain.LikedPostSort;
import com.leisure.postLike.dto.response.LikedPostListResponse;
import com.leisure.postLike.service.PostLikeQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PostLikeQueryController {

    private final PostLikeQueryService service;

    @GetMapping("/members/me/likes")
    public ResponseEntity<ApiResponse<LikedPostListResponse>> getLikedPosts(
            @CurrentMember String publicId,
            @RequestParam(defaultValue = "LATEST")LikedPostSort sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {

        LikedPostListResponse response = service.getLikedPosts(publicId, sort, page, size);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("좋아요한 게시글 목록 조회에 성공했습니다.", response));
    }
}
