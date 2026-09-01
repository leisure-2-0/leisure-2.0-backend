package com.leisure.festival.repository;

import com.leisure.festival.domain.Festival;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FestivalRepository extends JpaRepository<Festival, Long> {

    Optional<Festival> findByTourContentId(String tourContentId);

    List<Festival> findByOverviewIsNull();

    List<Festival> findByEventTimeIsNull();
}
