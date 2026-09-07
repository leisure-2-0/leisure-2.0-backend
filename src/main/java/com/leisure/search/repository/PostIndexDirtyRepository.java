package com.leisure.search.repository;

import com.leisure.search.domain.PostIndexDirty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostIndexDirtyRepository extends JpaRepository<PostIndexDirty, Long> {

    @Modifying
    @Query(value = """
            insert into post_index_dirties (post_id, version, retry_count, dirtied_at, next_retry_at)
            values (:postId, 1, 0, current_timestamp(6), null)
            on duplicate key update
                version = version + 1,
                retry_count = 0,
                dirtied_at = current_timestamp(6),
                next_retry_at = null
            """, nativeQuery = true)
    void markDirty(Long postId);

    @Query(value = "select * from post_index_dirties where next_retry_at is null or next_retry_at <= :now order by dirtied_at asc limit :size", nativeQuery = true)
    List<PostIndexDirty> findProcessable(LocalDateTime now, int size);

    @Modifying
    @Query("delete from PostIndexDirty d where d.postId = :postId and d.version = :version")
    int deleteIfVersionMatches(Long postId, long version);

    @Modifying
    @Query("update PostIndexDirty d set d.retryCount = d.retryCount + 1, d.nextRetryAt = :nextRetryAt where d.postId = :postId and d.version =: version")
    int markFailure(Long postId, long version, LocalDateTime nextRetryAt);
}
