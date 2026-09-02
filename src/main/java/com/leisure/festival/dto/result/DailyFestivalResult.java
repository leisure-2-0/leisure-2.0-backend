package com.leisure.festival.dto.result;

import com.leisure.festival.domain.FestivalCategory;

public record DailyFestivalResult(
        String code,

        String signguName,

        String name,

        String overview,

        String eventTime,

        String homepageUrl
) {}
