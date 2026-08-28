package com.leisure.post.controller;

import com.leisure.global.auth.CurrentMember;
import com.leisure.global.response.ApiResponse;
import com.leisure.post.domain.MyPostSort;
import com.leisure.post.domain.PostCategory;
import com.leisure.post.domain.PostSort;
import com.leisure.post.dto.response.*;
import com.leisure.post.service.PostQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(
        name = "게시글 조회(Post Query)",
        description = "내 게시글 목록, 내 임시저장 목록, 둘러보기 피드, 메인 피드, 게시글 상세 조회"
)
@RestController
@RequiredArgsConstructor
public class PostQueryController {

    private final PostQueryService service;

    @Operation(summary = "내 게시글 목록 조회", description = "본인이 게시(PUBLISHED)한 글을 오프셋 기반으로 조회한다.")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/members/me/posts")
    public ResponseEntity<ApiResponse<MyPostListResponse>> getMyPosts(
            @CurrentMember String publicId,
            @Parameter(description = "정렬 기준. LATEST=최신순, POPULAR=인기순")
            @RequestParam(defaultValue = "LATEST") MyPostSort sort,
            @Parameter(description = "페이지 번호(0부터 시작, 기본 0)")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기(1~30, 기본 15)")
            @RequestParam(required = false) Integer size
    ) {

        MyPostListResponse response = service.getMyPosts(publicId, sort, page, size);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("내 게시글 목록 조회에 성공했습니다.", response));
    }

    @Operation(summary = "둘러보기 피드 조회", description = "게시글을 커서 기반으로 조회한다. 비로그인 공개.")
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<PostListResponse>> getPosts(
            @CurrentMember(required = false) String publicId,
            @RequestParam(required = false) PostCategory category,
            @Parameter(description = "정렬 기준. LATEST=최신순, POPULAR=인기순")
            @RequestParam(defaultValue = "LATEST") PostSort sort,
            @Parameter(description = "이전 응답의 nextCursor를 그대로 전달. 첫 페이지는 생략")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "페이지 크기(1~30, 기본 15)")
            @RequestParam(required = false) Integer limit) {

        PostListResponse response = service.getPosts(publicId, category, sort, cursor, limit);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success( "둘러보기 게시글 목록 조회에 성공했습니다.", response));

    }

    @Operation(summary = "메인 피드 조회", description = "최신/인기순 상위 18개를 조회한다. 비로그인 공개.")
    @GetMapping("/posts/main")
    public ResponseEntity<ApiResponse<List<MainFeedPostResponse>>> getMainFeedPosts(
            @CurrentMember(required = false) String publicId,
            @RequestParam(required = false) PostCategory category,
            @Parameter(description = "정렬 기준. LATEST=최신순, POPULAR=인기순")
            @RequestParam(defaultValue = "LATEST") PostSort sort) {

        List<MainFeedPostResponse> response = service.getMainFeedPosts(publicId, category, sort);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("메인 게시글 목록 조회에 성공했습니다.", response));

    }

    @Operation(summary = "게시글 상세 조회", description = "게시글 상세를 조회하고 조회수를 1 증가시킨다. 비로그인 공개.")
    @GetMapping("/posts/{postId:\\d+}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> getPostDetail(@CurrentMember(required = false) String publicId, @PathVariable Long postId) {

        PostDetailResponse response = service.getPostDetail(publicId, postId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response.postId() + "번 게시글 조회에 성공했습니다", response));

    }

    @Operation(summary = "내 임시저장 목록 조회", description = "본인이 임시 저장(DRAFT)한 글을 최근 저장 순으로 조회한다.")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/members/me/drafts")
    public ResponseEntity<ApiResponse<List<DraftListResponse>>> getMyDrafts(@CurrentMember String publicId) {

        List<DraftListResponse> response = service.getMyDrafts(publicId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("임시 저장된 게시글 목록 조회에 성공했습니다.", response));
    }

    @Operation(summary = "임시 저장된 게시글 상세 조회", description = "본인이 임시 저장(DRAFT)한 글을 수정 화면 로드용으로 조회한다. 본인만 접근 가능.")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/members/me/drafts/{postId}")
    public ResponseEntity<ApiResponse<DraftDetailResponse>> getMyDraftDetail(@CurrentMember String publicId, @PathVariable Long postId) {

        DraftDetailResponse response = service.getMyDraftDetail(publicId, postId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response.postId() + "번 임시 저장된 게시글 조회에 성공했습니다", response));
    }
}
