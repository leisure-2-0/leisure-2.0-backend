package com.leisure.festival.repository;

import com.leisure.festival.domain.Festival;
import com.leisure.festival.domain.FestivalCategory;
import com.leisure.festival.dto.response.MonthlyFestivalResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FestivalRepository extends JpaRepository<Festival, Long>, FestivalRepositoryCustom {

    Optional<Festival> findByTourContentId(String tourContentId);

    List<Festival> findByOverviewIsNull();

    List<Festival> findByEventTimeIsNull();

//    @Query("""
//            select new com.leisure.festival.dto.response.MonthlyFestivalResponse(f.festivalId, f.name, f.eventTime, f.eventStartDate, f.eventEndDate)
//            from Festival f
//            where f.eventStartDate <= :monthEnd
//            and f.eventEndDate >= :monthStart
//            and (:code is null or f.lclsSystm2 = :code)
//            order by f.name asc
//            """)
//    List<MonthlyFestivalResponse> findMonthlyFestivals(LocalDate monthStart, LocalDate monthEnd, String code);

    @Query("""
            select new com.leisure.festival.dto.response.MonthlyFestivalResponse(f.festivalId, f.name, f.eventTime, f.eventStartDate, f.eventEndDate)
            from Festival f
            where ((f.eventStartDate >= :monthStart and f.eventStartDate <= :monthEnd)
            or (f.eventEndDate >= :monthStart and f.eventEndDate <= :monthEnd))
            and (:code is null or f.lclsSystm2 = :code)
            order by f.name asc
            """)
    List<MonthlyFestivalResponse> findMonthlyFestivals(LocalDate monthStart, LocalDate monthEnd, String code);
}
