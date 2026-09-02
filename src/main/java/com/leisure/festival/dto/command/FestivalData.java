package com.leisure.festival.dto.command;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record FestivalData(
        String tourContentId,

        String name,

        String address,

        String detailAddress,

        LocalDate eventStartDate,

        LocalDate eventEndDate,

        Double latitude,

        Double longitude,

        String ldongRegnCd,

        String ldongSignguCd,

        String contentTypeId,

        String lclsSystm2,

        String lclsSystm3,

        LocalDateTime tourModifiedAt,

        String thumbnailUrl
) {}
