package com.leisure.map.dto.response;

import com.leisure.post.domain.PostCategory;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "지도 범위 내 게시글 핀 응답")
public record MapPinResponse(
        @Schema(description = "게시글 ID")
        Long postId,

        @Schema(description = "게시글 제목")
        String title,

        @Schema(description = "게시글 카테고리")
        PostCategory category,

        @Schema(description = "게시글 위치 위도")
        double latitude,

        @Schema(description = "게시글 위치 경도")
        double longitude
) {}
