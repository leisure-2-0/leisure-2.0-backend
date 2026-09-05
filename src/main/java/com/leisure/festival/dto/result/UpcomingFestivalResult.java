package com.leisure.festival.dto.result;

import java.time.LocalDate;

public record UpcomingFestivalResult(
        Long festivalId,

        String name,

        String signguName,

        LocalDate eventStartDate,

        String thumbnailUrl
) {}
