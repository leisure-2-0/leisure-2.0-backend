package com.leisure.pointhistory.domain;

import com.leisure.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "point_histories",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_point_histories_actor_source_type",
                columnNames = {"actor_id", "source_id", "point_type"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class PointHistory extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "point_history_id")
    private Long pointHistoryId;

    @Column(name = "member_id", nullable = false, updatable = false, comment = "포인트 수령자(게시글 작성자) 회원 ID")
    private Long memberId;

    @Column(name = "amount", nullable = false, updatable = false, comment = "적립 포인트 (적립 시점 정책 점수 보존)")
    private int amount;

    @Column(name = "actor_id", nullable = false, updatable = false, comment = "행위자 ID (좋아요/북마크 누른 사람, 게시는 작성자 본인)")
    private Long actorId;

    @Column(name = "source_id", nullable = false, updatable = false, comment = "적립 트리거 대상 ID (게시글 ID)")
    private Long sourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "point_type", nullable = false, updatable = false, comment = "적립 사유 (POST_PUBLISH/LIKE_RECEIVED/BOOKMARK_RECEIVED)")
    private PointType pointType;

    private PointHistory(Long memberId, Long actorId, Long sourceId, PointType pointType) {
        this.memberId = memberId;
        this.amount = pointType.getAmount();
        this.actorId = actorId;
        this.sourceId = sourceId;
        this.pointType = pointType;
    }

    public static PointHistory create(Long memberId, Long actorId, Long sourceId, PointType pointType) {
        return new PointHistory(memberId, actorId, sourceId, pointType);
    }
}
