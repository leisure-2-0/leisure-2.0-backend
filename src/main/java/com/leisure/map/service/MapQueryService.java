package com.leisure.map.service;

import com.leisure.map.dto.response.MapPinResponse;
import com.leisure.map.dto.response.RegionPinCountResponse;
import com.leisure.map.repository.MapCustom;
import com.leisure.post.domain.PostCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MapQueryService {

    private final MapCustom repository; // RequiredArgsConstructor가 생성자 자동 생성_리포지토리 주입

    // 지역별 게시글 개수 조회 ==============================================================
    @Transactional(readOnly = true) // 트랜잭션을 읽기 전용으로 설정하여 성능 최적화
    public List<RegionPinCountResponse> getRegionPinCounts(PostCategory category) {
        // 쿼리가 바로 응답 DTO로 projection하므로 변환 단계 없이 그대로 반환
        return repository.findRegionPinCounts(category);
    }

    // 지도 범위 내 게시글 핀 조회 ==============================================================
    @Transactional(readOnly = true)
    public List<MapPinResponse> getPostPins(double minLat, double maxLat, double minLng, double maxLng, PostCategory category) {
        return repository.findPinsInBounds(minLat, maxLat, minLng, maxLng, category);
    }
}
