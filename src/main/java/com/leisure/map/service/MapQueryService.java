package com.leisure.map.service;

import com.leisure.map.dto.response.PostMapPinResponse;
import com.leisure.map.dto.response.RegionPinCountResponse;
import com.leisure.post.domain.PostCategory;
import com.leisure.post.dto.result.PostPinResult;
import com.leisure.post.dto.result.RegionPinCountResult;
import com.leisure.post.service.PostReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MapQueryService {
    private final PostReader postReader; // RequiredArgsConstructor가 생성자 자동 생성_서비스 주입


    // 지역별 게시글 개수 조회 ==============================================================
    @Transactional(readOnly = true) // 트랜잭션을 읽기 전용으로 설정하여 성능 최적화
    public List<RegionPinCountResponse> getRegionPinCounts(PostCategory category) {
        // 지역별 게시글 개수 조회 -> 결과를 RegionPinCountResponse로 변환하여 반환
        return postReader.findRegionPinCounts(category).stream()
                .map(this::toRegionPinCountResponse) // #37 _ 각 요소 뜯어서 변환
                .toList(); // 다시 리스트로 합치기
    }


    // 지도 범위 내 게시글 핀 조회 ==============================================================
    @Transactional(readOnly = true)
    public List<PostMapPinResponse> getPostPins(double minLat, double maxLat, double minLng, double maxLng, PostCategory category) {
        // 지도 범위 내 게시글 핀 조회 -> 결과를 PostMapPinResponse로 변환하여 반환
        return postReader.findPinsInBounds(minLat, maxLat, minLng, maxLng, category).stream()
                .map(this::toPostMapPinResponse) // #45 _ 각 요소 뜯어서 변환
                .toList();
    }


    private RegionPinCountResponse toRegionPinCountResponse(RegionPinCountResult result) {
        return new RegionPinCountResponse(result.region(), result.postCount(), result.centerLat(), result.centerLng());
    }


    private PostMapPinResponse toPostMapPinResponse(PostPinResult result) {
        return new PostMapPinResponse(result.postId(), result.title(), result.category(), result.latitude(), result.longitude());
    }
}
