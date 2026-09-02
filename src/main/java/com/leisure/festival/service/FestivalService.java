package com.leisure.festival.service;

import com.leisure.festival.domain.Festival;
import com.leisure.festival.dto.command.FestivalData;
import com.leisure.festival.dto.result.FestivalSyncResult;
import com.leisure.festival.repository.FestivalRepository;
import com.leisure.global.external.tourapi.TourApiClient;
import com.leisure.global.external.tourapi.dto.response.FestivalDetailCommonResponse;
import com.leisure.global.external.tourapi.dto.response.FestivalDetailIntroResponse;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class FestivalService {

    private static final Logger log = LoggerFactory.getLogger(FestivalService.class);

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final Pattern HREF_PATTERN = Pattern.compile("href=\"([^\"]+)\"");

    private final FestivalRepository repository;

    private final TourApiClient client;

    private final FestivalWriter writer;

    public void syncFestivalList() {

        String eventStartDate = LocalDate.now()
                .withDayOfYear(1)
                .format(DateTimeFormatter.BASIC_ISO_DATE);

        List<Item> items = client.fetchFestivals(eventStartDate);

        List<FestivalData> dataList = items.stream()
                .map(item -> toData(item))
                .toList();

        FestivalSyncResult result = writer.updates(dataList);

        log.info("[festival-sync] 목록 완료 inserted={}, updated={}, total={}",
                result.inserted(), result.updated(), result.total());
    }

    public void syncOverviewAndHomepage() {

        List<Festival> festivals = repository.findByOverviewIsNull();

        int success = 0, failed = 0;

        for (Festival festival : festivals) {
            throttle();
            String contentId = festival.getTourContentId();
            try {
                FestivalDetailCommonResponse detail = client.fetchDetailCommon(contentId);

                FestivalDetailCommonResponse.Response response = detail.response();
                if (response == null || response.body() == null || response.body().items() == null
                        || response.body().items().item() == null || response.body().items().item().isEmpty()) {
                    failed++;
                    continue;
                }

                FestivalDetailCommonResponse.Item item = response.body().items().item().get(0);
                writer.updateDetailCommon(contentId, blankToNull(item.overview()), normalizeHomepage(item.homepage()));
                success++;
            } catch (Exception e) {
                log.warn("[festival-detail] 소개글 보강 실패 contentId={}", contentId, e);
                failed++;
            }
        }

        log.info("[festival-detail] 소개글 보강 완료 처리={}, 실패={}", success, failed);
    }

    public void syncEventTime() {

        List<Festival> festivals = repository.findByEventTimeIsNull();

        int success = 0, failed = 0;

        for (Festival festival : festivals) {
            throttle();
            String contentId = festival.getTourContentId();
            try {
                FestivalDetailIntroResponse detail = client.fetchDetailIntro(contentId);

                FestivalDetailIntroResponse.Response response = detail.response();
                if (response == null || response.body() == null || response.body().items() == null
                        || response.body().items().item() == null || response.body().items().item().isEmpty()) {
                    failed++;
                    continue;
                }

                FestivalDetailIntroResponse.Item item = response.body().items().item().get(0);
                writer.updateDetailIntro(contentId, blankToNull(item.playtime()));
                success++;
            } catch (Exception e) {
                log.warn("[festival-detail] 운영시간 보강 실패 contentId={}", contentId, e);
                failed++;
            }
        }

        log.info("[festival-detail] 운영시간 보강 완료 처리={}, 실패={}", success, failed);
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
                parseDateTime(item.modifiedTime()),
                blankToNull(item.firstImage2())
        );
    }

    private String blankToNull(String string) {

        if (string == null || string.isBlank()) {
            return null;
        }

        return string;
    }

    private String normalizeHomepage(String homepage) {

        if (homepage == null || homepage.isBlank()) {
            return null;
        }

        Matcher matcher = HREF_PATTERN.matcher(homepage);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return homepage.trim();
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

    private void throttle() {
        try {
            Thread.sleep(1250);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
