package com.leisure.map.repository;

import com.leisure.map.dto.response.MapPinResponse;
import com.leisure.map.dto.response.RegionPinCountResponse;
import com.leisure.post.domain.PostCategory;

import java.util.List;

public interface MapRepositoryCustom {

    List<RegionPinCountResponse> findRegionPinCounts(PostCategory category);

    List<MapPinResponse> findPinsInBounds(double minLat, double maxLat, double minLng, double maxLng, PostCategory category);
}
