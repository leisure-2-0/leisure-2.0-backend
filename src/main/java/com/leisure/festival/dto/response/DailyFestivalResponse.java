package com.leisure.festival.dto.response;

import com.leisure.festival.domain.FestivalCategory;

public record DailyFestivalResponse(
        FestivalCategory category,

        String signguName,

        String name,

        String overview,

        String eventTime,

        String homepageUrl
) {
}
