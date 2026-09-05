package com.leisure.festival.repository;

import com.leisure.festival.dto.result.DailyFestivalResult;
import com.leisure.festival.dto.result.UpcomingFestivalResult;

import java.time.LocalDate;
import java.util.List;

public interface FestivalRepositoryCustom {

    List<DailyFestivalResult> findDailyFestivals(LocalDate date, String code);

    List<UpcomingFestivalResult> findUpcomingFestivals(LocalDate today);

}
