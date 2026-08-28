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

    // TODO: 부하 테스트 후 Redis 조회수 INCR
    @Modifying(clearAutomatically = true)
    @Query("""
            update Post p
            set p.viewCount = p.viewCount + 1
            where p.postId = :postId
            and p.deletedAt is null
            and p.status = com.leisure.post.domain.PostStatus.PUBLISHED
           """)
    void increaseViewCount(Long postId);


    // flushAutomatically = true로 @Modifying 레벨에서 flush를 위임할 수도 있지만,
    // 현재는 save 직후 명시적으로 flush해서 unique constraint 예외를 즉시 처리한다.
    @Modifying(clearAutomatically = true)
    @Query("""
            update Post p
            set p.likeCount = p.likeCount + 1
            where p.postId = :postId
            and p.deletedAt is null
            and p.status = com.leisure.post.domain.PostStatus.PUBLISHED
            """)
    void increaseLikeCount(Long postId);

    @Modifying(clearAutomatically = true)
    @Query("""
            update Post p
            set p.likeCount = p.likeCount - 1
            where p.postId = :postId
            and p.deletedAt is null
            and p.status = com.leisure.post.domain.PostStatus.PUBLISHED
            and p.likeCount > 0
            """)
    void decreaseLikeCount(Long postId);

    @Modifying(clearAutomatically = true)
    @Query("""
            update Post p
            set p.bookmarkCount = p.bookmarkCount + 1
            where p.postId = :postId
            and p.deletedAt is null
            and p.status = com.leisure.post.domain.PostStatus.PUBLISHED
            """)
    void increaseBookmarkCount(Long postId);

    @Modifying(clearAutomatically = true)
    @Query("""
            update Post p
            set p.bookmarkCount = p.bookmarkCount - 1
            where p.postId = :postId
            and p.deletedAt is null
            and p.status = com.leisure.post.domain.PostStatus.PUBLISHED
            and p.bookmarkCount > 0
            """)
    void decreaseBookmarkCount(Long postId);
}
