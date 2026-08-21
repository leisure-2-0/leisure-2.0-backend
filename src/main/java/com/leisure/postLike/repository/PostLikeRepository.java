package com.leisure.postLike.repository;

import com.leisure.postLike.domain.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {


    boolean existsByMemberIdAndPostId(Long memberId, Long postId);

    Optional<PostLike> findByMemberIdAndPostId(Long memberId, Long postId);

    @Modifying
    @Query("delete from PostLike pl where pl.memberId = :memberId and pl.postId = :postId")
    int deleteByMemberIdAndPostId(Long memberId, Long postId);
}
