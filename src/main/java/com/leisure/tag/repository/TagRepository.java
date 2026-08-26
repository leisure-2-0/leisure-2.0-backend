package com.leisure.tag.repository;

import com.leisure.tag.domain.PostTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface TagRepository extends JpaRepository<PostTag, Long> {

    List<PostTag> findByPostIdIn(Collection<Long> postId);

    /**
     * 특정 게시글의 태그를 전부 삭제한다 -> 태그 교체 시 "삭제 후 재삽입"의 삭제
     * Spring Data JPA의 delete(deleteByPostId 자동 생성)를 쓰지 않고 벌크 delete 쿼리를 직접 쓴 이유:
     * delete는 대상 엔티티를 먼저 SELECT로 전부 조회한 뒤 영속성 컨텍스트를 거쳐
     * 하나씩 삭제한다(SELECT 1 + DELETE N). 반면 이 벌크 delete는 SELECT 없이
     * DELETE 한 방으로 끝나 태그 교체 시 불필요한 조회, 개별 삭제를 피한다.
     * 벌크 연산은 영속성 컨텍스트를 우회하므로, 같은 트랜잭션에서 이후 조회가 필요하면 주의
     */
    @Modifying
    @Query("delete from PostTag pt where pt.postId = :postId")
    void deleteByPostId(Long postId);
}
