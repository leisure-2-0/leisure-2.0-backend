package com.leisure.member.repository;

import com.leisure.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByEmailAndDeletedAtIsNull(String email);

    boolean existsByNicknameAndDeletedAtIsNull(String nickname);

    Optional<Member> findByPublicIdAndDeletedAtIsNull(String publicId);

    Optional<Member> findByEmailAndDeletedAtIsNull(String email);
}
