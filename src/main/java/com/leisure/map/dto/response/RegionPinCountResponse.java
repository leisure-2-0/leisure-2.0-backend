package com.leisure.map.dto.response;

public record RegionPinCountResponse(
        String region, // 지역 이름
        long postCount, // 해당 지역의 게시글 개수
        double centerLat, // 해당 지역의 중심 위도
        double centerLng // 해당 지역의 중심 경도
) {
}
