package com.leisure.post.repository;

import com.leisure.post.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>, PostCustom {

    Optional<Post> findByPostIdAndDeletedAtIsNull(Long postId);

    @Query("""
            select p.likeCount
            from Post p
            where p.postId = :postId
            and p.deletedAt is null
            and p.status = com.leisure.post.domain.PostStatus.PUBLISHED
            """)
    int findLikeCountByPostId(Long postId);

    @Query("""
            select p.bookmarkCount
            from Post p
            where p.postId = :postId
            and p.deletedAt is null
            and p.status = com.leisure.post.domain.PostStatus.PUBLISHED
            """)
    int findBookmarkCountByPostId(Long postId);

    @Query("""
            select count(p.memberId)
            from Post p
            where p.memberId = :memberId
            and p.deletedAt is null
            and p.status = com.leisure.post.domain.PostStatus.PUBLISHED
            """)
    long countMyPosts(Long memberId);

    @Modifying(clearAutomatically = true)
    @Query("update Post p set p.likeCount = p.likeCount + 1 where p.postId = :postId")
    void increaseLikeCount(Long postId);

    @Modifying(clearAutomatically = true)
    @Query("update Post p set p.likeCount = p.likeCount - 1 where p.postId = :postId and p.likeCount > 0")
    void decreaseLikeCount(Long postId);

    @Modifying(clearAutomatically = true)
    @Query("update Post p set p.bookmarkCount = p.bookmarkCount + 1 where p.postId = :postId")
    void increaseBookmarkCount(Long postId);

    @Modifying(clearAutomatically = true)
    @Query("update Post p set p.bookmarkCount = p.bookmarkCount - 1 where p.postId = :postId and p.bookmarkCount > 0")
    void decreaseBookmarkCount(Long postId);
}
