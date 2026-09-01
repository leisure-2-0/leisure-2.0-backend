package com.leisure.festival.service;

import com.leisure.festival.dto.FestivalData;
import com.leisure.festival.dto.result.FestivalSyncResult;
import com.leisure.global.external.tourapi.TourApiClient;
import com.leisure.global.external.tourapi.dto.response.FestivalListResponse.Item;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FestivalService {

    private static final Logger log = LoggerFactory.getLogger(FestivalService.class);

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final TourApiClient client;

    private final FestivalUpserter upserter;

    public FestivalSyncResult syncFestivals() {

        String eventStartDate = LocalDate.now()
                .withDayOfYear(1)
                .format(DateTimeFormatter.BASIC_ISO_DATE);

        List<Item> items = client.searchAllFestivals(eventStartDate);

        List<FestivalData> dataList = items.stream()
                .map(item -> toData(item))
                .toList();

        return upserter.upsert(dataList);
    }

    private FestivalData toData(Item item) {
        return new FestivalData(
                item.contentId(),
                item.title(),
                blankToNull(item.addr1()),
                blankToNull(item.addr2()),
                parseDate(item.eventStartDate(), item.contentId()),
                parseDate(item.eventEndDate(), item.contentId()),
                parseCoord(item.mapy()),   // 위도
                parseCoord(item.mapx()),   // 경도 (mapx/mapy 교차 매핑)
                blankToNull(item.ldongRegnCd()),
                blankToNull(item.ldongSignguCd()),
                blankToNull(item.contentTypeId()),
                blankToNull(item.lclsSystm2()),
                blankToNull(item.lclsSystm3()),
                parseDateTime(item.modifiedTime())
        );
    }

    private String blankToNull(String string) {
        if (string == null || string.isBlank()) {
            return null;
        }
        return string;
    }

    private LocalDate parseDate(String raw, String contentId) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw, DateTimeFormatter.BASIC_ISO_DATE);
        } catch (DateTimeParseException e) {
            log.warn("[festival-sync] 날짜 파싱 실패 contentId={}, raw={}", contentId, raw);
            return null;
        }
    }

    private Double parseCoord(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
