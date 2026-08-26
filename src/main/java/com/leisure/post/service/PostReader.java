package com.leisure.post.service;

import com.leisure.post.domain.PostCategory;
import com.leisure.post.dto.result.PostPinResult;
import com.leisure.post.dto.result.RegionPinCountResult;
import com.leisure.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

// MemberReader와 같은 역할 — 다른 도메인(map 등)이 게시글 데이터를 조회할 때 쓰는 창구.
@Component
@RequiredArgsConstructor
public class PostReader {
    private final PostRepository repository;


    // 지역별 게시글 개수 조회 ==============================================================
    public List<RegionPinCountResult> findRegionPinCounts(PostCategory category) {
        // 지역별 게시글 개수를 조회 -> 결과 반환
        return repository.findRegionPinCounts(category);
    }   

    
    // 지도 범위 내 게시글 핀 조회 ==============================================================
    public List<PostPinResult> findPinsInBounds(double minLat, double maxLat, double minLng, double maxLng, PostCategory category) {
        // 지도 범위 내 게시글 핀을 조회 -> 결과 반환
        return repository.findPinsInBounds(minLat, maxLat, minLng, maxLng, category);
    }
}
