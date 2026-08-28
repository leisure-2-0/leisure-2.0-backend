package com.leisure.map.controller;

import com.leisure.global.response.ApiResponse;
import com.leisure.map.dto.response.MapPinResponse;
import com.leisure.map.dto.response.RegionPinCountResponse;
import com.leisure.map.service.MapQueryService;
import com.leisure.post.domain.PostCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController //컨트롤러 명시
@RequiredArgsConstructor //생성자 주입
public class MapController {
    private final MapQueryService service; //RequiredArgsConstructor가 생성자 자동 생성_서비스 주입


    // ================ 1. 지역별 게시글 집계 조회 API :: 클러스터 핀 ================
    @GetMapping("/maps/regions")
    public ResponseEntity<ApiResponse<List<RegionPinCountResponse>>> getRegionPinCounts( //지역별 게시글 개수 리스트
            @RequestParam(required = false) PostCategory category //선택적 카테고리 필터
    ) {
        // 서비스 호출하여 지역별 게시글 개수 조회
        List<RegionPinCountResponse> response = service.getRegionPinCounts(category);

        // ResponseEntity를 사용하여 HTTP 응답 생성
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("지역별 게시글 집계 조회에 성공했습니다.", response));
    }


    // ================ 2. 지도 범위 내 게시글 핀 조회 API :: 개별 핀 ====================
    @GetMapping("/maps/pins")
    public ResponseEntity<ApiResponse<List<MapPinResponse>>> getPostPins(
            @RequestParam double minLat, // 최소 위도
            @RequestParam double maxLat, // 최대 위도 
            @RequestParam double minLng, // 최소 경도
            @RequestParam double maxLng, // 최대 경도
            @RequestParam(required = false) PostCategory category // 선택적 카테고리 필터
    ) {
        // 서비스 호출하여 지도 범위 내 게시글 핀 조회
        List<MapPinResponse> response = service.getPostPins(minLat, maxLat, minLng, maxLng, category);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("지도 범위 내 게시글 핀 조회에 성공했습니다.", response));
    }
}
