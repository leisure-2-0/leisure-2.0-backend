package com.leisure.festival.dto.response;

import java.time.LocalDate;

public record UpcomingFestivalResponse(
        Long festivalId,

        String name,

        String region,

        LocalDate eventStartDate,

        String thumbnailUrl
) {}
