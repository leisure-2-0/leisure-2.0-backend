package com.leisure.search.controller;

import com.leisure.global.auth.CurrentMember;
import com.leisure.global.response.ApiResponse;
import com.leisure.post.domain.PostCategory;
import com.leisure.search.dto.response.PostSearchResponse;
import com.leisure.search.service.PostSearchService;
import com.leisure.search.service.SearchSort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "게시글 검색", description = "Elasticsearch 기반 게시글 검색 API")
@RestController
@RequiredArgsConstructor
public class PostSearchController {

    private final PostSearchService postSearchService;

    @Operation(
            summary = "게시글 검색",
            description = "검색어(searchTerm)로 제목·태그·지역을 매칭해 게시글을 찾는다(비로그인 공개). "
                    + "category는 선택 필터(없으면 전체), sort 미지정 시 관련도순. 오프셋 페이지네이션이며 "
                    + "결과가 너무 깊은 페이지(from+size > 10000)면 SEARCH_PAGE_TOO_DEEP."
    )
    @GetMapping("/searches")
    public ResponseEntity<ApiResponse<PostSearchResponse>> search(
            @CurrentMember(required = false) String publicId,
            @Parameter(description = "검색어 — 제목·태그·지역에 걸쳐 매칭(필수)")
            @RequestParam String searchTerm,
            @Parameter(description = "카테고리 필터 — 없으면 전체에서 검색")
            @RequestParam(required = false) PostCategory category,
            @Parameter(description = "정렬 기준 — 미지정 시 관련도순(ACCURACY)")
            @RequestParam(required = false, defaultValue = "ACCURACY") SearchSort sort,
            @Parameter(description = "페이지 번호(0-base) — 미지정 시 0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기(1~30) — 미지정 시 15")
            @RequestParam(required = false) Integer size) {

        PostSearchResponse response = postSearchService.search(publicId, searchTerm, category, sort, page, size);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("게시글 검색 성공", response));
    }
}
