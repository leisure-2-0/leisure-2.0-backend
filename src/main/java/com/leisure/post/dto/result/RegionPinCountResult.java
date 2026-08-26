package com.leisure.post.dto.result;

public record RegionPinCountResult(
        String region,
        long postCount,
        
        double centerLat,
        double centerLng
) {
}
