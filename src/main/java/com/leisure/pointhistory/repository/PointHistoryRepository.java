package com.leisure.pointhistory.repository;

import com.leisure.pointhistory.domain.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    // 유니크(actor_id, source_id, point_type) 위반 시 무시(0행), 신규면 1행 → 원자적 멱등 적립
    @Modifying
    @Query(value = """
            insert ignore into point_histories (member_id, actor_id, source_id, point_type, amount, created_at)
            values (:memberId, :actorId, :sourceId, :pointType, :amount, now())
            """, nativeQuery = true)
    int insertIfAbsent(Long memberId, Long actorId, Long sourceId, String pointType, int amount);

    @Modifying
    @Query("delete from PointHistory ph where ph.memberId in :memberIds")
    void deleteByMemberIdIn(Collection<Long> memberIds);
}
