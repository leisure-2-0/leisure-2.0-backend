package com.leisure.member.repository;

import com.leisure.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByEmailAndDeletedAtIsNull(String email);

    boolean existsByNicknameAndDeletedAtIsNull(String nickname);

    Optional<Member> findByPublicIdAndDeletedAtIsNull(String publicId);

    Optional<Member> findByMemberIdAndDeletedAtIsNull(Long memberId);

    Optional<Member> findByEmailAndDeletedAtIsNull(String email);

    @Query("""
           select count(m.memberId)
           from Member m
           where m.createdAt >= :start
           and m.createdAt < :end
           and m.deletedAt is null
        """)
    long countJoinedBetween(LocalDateTime start, LocalDateTime end);

    @Modifying
    @Query("update Member m set m.point = m.point + :amount where m.memberId = :memberId and m.deletedAt is null")
    int addPoint(Long memberId, int amount);
}
