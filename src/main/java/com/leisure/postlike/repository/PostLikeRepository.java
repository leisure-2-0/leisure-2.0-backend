package com.leisure.postlike.repository;

import com.leisure.postlike.domain.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long>, PostLikeRepositoryCustom {


    boolean existsByMemberIdAndPostId(Long memberId, Long postId);

    @Modifying
    @Query("delete from PostLike pl where pl.memberId = :memberId and pl.postId = :postId")
    int deleteByMemberIdAndPostId(Long memberId, Long postId);

    @Query("""
         select count(pl.postId)
         from PostLike pl
         join Post p
         on pl.postId = p.postId
         join Member m
         on p.memberId = m.memberId
         where pl.memberId = :memberId
         and p.deletedAt is null
         and m.deletedAt is null
         and p.status = com.leisure.post.domain.PostStatus.PUBLISHED
    """)
    long countLikedPosts(Long memberId);

    @Modifying
    @Query("delete from PostLike pl where pl.postId in :postIds")
    void deleteByPostIdIn(Collection<Long> postIds);

    @Modifying
    @Query("delete from PostLike pl where pl.memberId in :memberIds")
    void deleteByMemberIdIn(Collection<Long> memberIds);
}
