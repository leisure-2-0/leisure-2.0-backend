package com.leisure.post.controller;

import com.leisure.global.auth.CurrentMember;
import com.leisure.global.response.ApiResponse;
import com.leisure.post.domain.MyPostSort;
import com.leisure.post.domain.PostCategory;
import com.leisure.post.domain.PostSort;
import com.leisure.post.dto.response.MyPostListResponse;
import com.leisure.post.dto.response.PostDetailResponse;
import com.leisure.post.dto.result.PostDetailResult;
import com.leisure.post.dto.response.PostListResponse;
import com.leisure.post.service.PostQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<PostListResponse>> getPosts(
            @CurrentMember(required = false) String publicId,
            @RequestParam(required = false) PostCategory category,
            @RequestParam(defaultValue = "LATEST") PostSort sort,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {

        PostListResponse response = service.getPosts(publicId, category, sort, cursor, limit);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success( "게시글 목록 조회에 성공했습니다.", response));

    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> getPostDetail(@CurrentMember(required = false) String publicId, @PathVariable Long postId) {

        PostDetailResult result = service.getPostDetail(publicId, postId);

        PostDetailResponse response = new PostDetailResponse(
                result.postId(),
                result.title(),
                result.content(),
                result.category(),
                result.viewCount(),
                result.likeCount(),
                result.bookmarkCount(),
                result.isMine(),
                result.isLiked(),
                result.isBookmarked(),
                result.publishedAt(),
                new PostDetailResponse.AuthorResponse(
                        result.author().memberId(),
                        result.author().nickname(),
                        result.author().profileImageUrl()
                )
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response.postId() + "번 게시글 조회에 성공했습니다", response));

    }
}
