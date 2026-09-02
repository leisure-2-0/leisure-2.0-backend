package com.leisure.festival.service;

import com.leisure.festival.domain.FestivalCategory;
import com.leisure.festival.dto.response.DailyFestivalResponse;
import com.leisure.festival.dto.response.MonthlyFestivalResponse;
import com.leisure.festival.dto.response.UpcomingFestivalResponse;
import com.leisure.festival.dto.result.DailyFestivalResult;
import com.leisure.festival.dto.result.UpcomingFestivalResult;
import com.leisure.festival.repository.FestivalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FestivalQueryService {

    private final FestivalRepository repository;

    @Transactional(readOnly = true)
    public List<MonthlyFestivalResponse> getMonthlyFestivals(int year, int month, FestivalCategory category) {

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        String code = (category != null) ? category.getCode() : null;

        return repository.findMonthlyFestivals(monthStart, monthEnd, code);
    }

    @Transactional(readOnly = true)
    public List<DailyFestivalResponse> getDailyFestivals(LocalDate date, FestivalCategory category) {

        String code = (category != null) ? category.getCode() : null;

        List<DailyFestivalResult> results = repository.findDailyFestivals(date, code);

        List<DailyFestivalResponse> responses = new ArrayList<>();

        for (DailyFestivalResult result : results) {
            String region = toShortRegionName(result.signguName());

            DailyFestivalResponse response =
                    new DailyFestivalResponse(FestivalCategory.fromCode(result.code()),
                            region, result.name(), result.overview(), result.eventTime(), result.homepageUrl());

            responses.add(response);
        }

        return responses;
    }

    private String toShortRegionName(String name) {

        if (name == null) {
            return null;
        }

        if (name.contains(" ")) {
            return name;
        }

        if (name.equals("세종특별자치시")) {
            return "세종";
        }

        return name.replaceAll("^(..+)(시|군|구)$", "$1");
    }

    @Transactional(readOnly = true)
    public List<UpcomingFestivalResponse> getUpcomingFestivals() {
        LocalDate today = LocalDate.now();

        List<UpcomingFestivalResult> results = repository.findUpcomingFestivals(today);

        List<UpcomingFestivalResponse> responses = new ArrayList<>();

        for (UpcomingFestivalResult result : results) {
            String region = toShortRegionName(result.signguName());

            UpcomingFestivalResponse response =
                    new UpcomingFestivalResponse(result.festivalId(), result.name(), region, result.eventStartDate(), result.thumbnailUrl());

            responses.add(response);
        }

        return responses;
    }
}
