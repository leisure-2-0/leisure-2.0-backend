package com.leisure.Bookmark.repository;

import com.leisure.Bookmark.domain.PostBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BookmarkRepository extends JpaRepository<PostBookmark, Long> {

    boolean existsByMemberIdAndPostId(Long memberId, Long postId);

    @Modifying
    @Query("delete from PostBookmark pb where pb.memberId = :memberId and pb.postId = :postId")
    int deleteByMemberIdAndPostId(Long memberId, Long postId);
}
