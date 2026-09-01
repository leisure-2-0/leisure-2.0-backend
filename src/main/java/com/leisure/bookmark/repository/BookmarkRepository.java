package com.leisure.bookmark.repository;

import com.leisure.bookmark.domain.PostBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BookmarkRepository extends JpaRepository<PostBookmark, Long>, BookmarkRepositoryCustom {

    boolean existsByMemberIdAndPostId(Long memberId, Long postId);

    @Modifying
    @Query("delete from PostBookmark pb where pb.memberId = :memberId and pb.postId = :postId")
    int deleteByMemberIdAndPostId(Long memberId, Long postId);

    @Query("""
         select count(pb.postId)
         from PostBookmark pb
         join Post p
         on pb.postId = p.postId
         join Member m
         on p.memberId = m.memberId
         where pb.memberId = :memberId
         and p.deletedAt is null
         and m.deletedAt is null
         and p.status = com.leisure.post.domain.PostStatus.PUBLISHED
    """)
    long countBookmarkedPosts(Long memberId);
}
