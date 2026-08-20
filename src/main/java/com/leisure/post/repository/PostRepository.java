package com.leisure.post.repository;

import com.leisure.post.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findByPostIdAndDeletedAtIsNull(Long postId);
}
