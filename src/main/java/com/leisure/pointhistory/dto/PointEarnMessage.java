package com.leisure.pointhistory.dto;

import com.leisure.pointhistory.domain.PointType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "포인트 적립 메시지 (RabbitMQ 도메인 이벤트 페이로드)")
public record PointEarnMessage(

        @Schema(description = "포인트 수령자(게시글 작성자) 회원 ID")
        Long memberId,

        @Schema(description = "행위자 ID (좋아요/북마크 누른 사람, 게시는 작성자 본인)")
        Long actorId,

        @Schema(description = "적립 트리거 대상 ID (게시글 ID)")
        Long sourceId,

        @Schema(description = "적립 사유/점수 (POST_PUBLISH/LIKE_RECEIVED/BOOKMARK_RECEIVED)")
        PointType pointType
) {}
