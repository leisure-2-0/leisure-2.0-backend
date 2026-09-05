package com.leisure.festival.dto.response;

import java.time.LocalDate;

public record MonthlyFestivalResponse(
    Long festivalId,

    String name,

    String eventTime,

    LocalDate eventStartDate,

    LocalDate eventEndDate
) {}
