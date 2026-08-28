package com.leisure.map.controller;

import com.leisure.global.response.ApiResponse;
import com.leisure.map.dto.response.MapPinResponse;
import com.leisure.map.dto.response.RegionPinCountResponse;
import com.leisure.map.service.MapQueryService;
import com.leisure.post.domain.PostCategory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(
        name = "지도(Map)",
        description = "지역별 핀 집계와 지도 bounds 내 게시글 핀 조회"
)
@RestController
@RequiredArgsConstructor
public class MapQueryController {

    private final MapQueryService service;


    @Operation(
            summary = "지역별 핀 집계 조회",
            description = "게시글을 region 기준으로 묶어 게시글 수와 게시글 좌표 평균 중심을 반환한다. 비로그인 공개."
    )
    @GetMapping("/maps/regions")
    public ResponseEntity<ApiResponse<List<RegionPinCountResponse>>> getRegionPinCounts(
            @RequestParam(required = false) PostCategory category // 선택적 카테고리 필터
    ) {
        List<RegionPinCountResponse> response = service.getRegionPinCounts(category);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("지역별 게시글 집계 조회에 성공했습니다.", response));
    }


    @Operation(
            summary = "지도 범위 내 게시글 핀 조회",
            description = "현재 지도 bounds 안의 게시글 핀을 최대 500개 조회한다. bounds 좌표가 유효하지 않거나 서버가 허용한 화면 크기보다 넓으면 400을 반환한다."
    )
    @GetMapping("/maps/pins")
    public ResponseEntity<ApiResponse<List<MapPinResponse>>> getPostPins(
            @RequestParam double minLat, // 최소 위도
            @RequestParam double maxLat, // 최대 위도
            @RequestParam double minLng, // 최소 경도
            @RequestParam double maxLng, // 최대 경도
            @RequestParam(required = false) PostCategory category // 선택적 카테고리 필터
    ) {
        List<MapPinResponse> response = service.getPostPins(minLat, maxLat, minLng, maxLng, category);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("지도 범위 내 게시글 핀 조회에 성공했습니다.", response));
    }
}
