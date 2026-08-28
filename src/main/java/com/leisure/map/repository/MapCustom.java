package com.leisure.map.repository;

import com.leisure.map.dto.response.MapPinResponse;
import com.leisure.map.dto.response.RegionPinCountResponse;
import com.leisure.post.domain.PostCategory;

import java.util.List;

// 기능 목록 _ 실제 구현은 MapCustomImpl에서 수행
public interface MapCustom {

    List<RegionPinCountResponse> findRegionPinCounts(PostCategory category);

    List<MapPinResponse> findPinsInBounds(double minLat, double maxLat, double minLng, double maxLng, PostCategory category);
}
