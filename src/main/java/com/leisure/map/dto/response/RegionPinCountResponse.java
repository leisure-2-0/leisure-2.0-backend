package com.leisure.map.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "지역별 지도 핀 집계 응답")
public record RegionPinCountResponse(
        @Schema(description = "지역 이름")
        String region,

        @Schema(description = "해당 지역의 게시글 개수")
        long postCount,

        @Schema(description = "해당 지역 게시글 좌표 평균 기준 중심 위도")
        double centerLat,

        @Schema(description = "해당 지역 게시글 좌표 평균 기준 중심 경도")
        double centerLng
) {}
