package com.leisure.post.controller;

import com.leisure.global.auth.CurrentMember;
import com.leisure.global.response.ApiResponse;
import com.leisure.post.domain.MyPostSort;
import com.leisure.post.dto.response.MyPostListResponse;
import com.leisure.post.service.PostQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PostQueryController {

    private final PostQueryService service;

    @GetMapping("/members/me/posts")
    public ResponseEntity<ApiResponse<MyPostListResponse>> getMyPosts(
            @CurrentMember String publicId,
            @RequestParam(defaultValue = "LATEST") MyPostSort sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {

        MyPostListResponse response = service.getMyPosts(publicId, sort, page, size);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("내 게시글 목록 조회에 성공했습니다.", response));
    }
}
