package com.leisure.global.external.tourapi;

import com.leisure.global.external.tourapi.dto.response.FestivalDetailCommonResponse;
import com.leisure.global.external.tourapi.dto.response.FestivalDetailIntroResponse;
import com.leisure.global.external.tourapi.dto.response.FestivalListResponse;
import com.leisure.global.external.tourapi.dto.response.LdongCodeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.leisure.global.external.tourapi.dto.response.FestivalListResponse.*;

@Component
@EnableConfigurationProperties(TourApiProperties.class)
public class TourApiClient {

    private static final Logger log = LoggerFactory.getLogger(TourApiClient.class);

    private final RestClient client;

    private final TourApiProperties properties;

    public TourApiClient(RestClient client, TourApiProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public LdongCodeResponse fetchRegions() {
        URI uri = buildUri("/ldongCode2", null);
        return getWithRetry(uri, LdongCodeResponse.class);
    }

    public LdongCodeResponse fetchSigungus(String lDongRegnCd) {
        URI uri = buildUri("/ldongCode2", lDongRegnCd);
        return getWithRetry(uri, LdongCodeResponse.class);
    }

    private URI buildUri(String path, String lDongRegnCd) {

        if (!StringUtils.hasText(properties.serviceKey())) {
            throw new TourApiException("[tour-api] serviceKey가 비어 있다. TOUR_API_SERVICE_KEY(tour-api.service-key)를 설정하라.");
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(properties.baseUrl())
                .path(path)
                .queryParam("serviceKey", properties.serviceKey())
                .queryParam("MobileOS", "WEB")
                .queryParam("MobileApp", "ProjectY")
                .queryParam("numOfRows", 100)
                .queryParam("pageNo", 1)
                .queryParam("_type", "json");

        if (StringUtils.hasText(lDongRegnCd)) {
            builder.queryParam("lDongRegnCd", lDongRegnCd);
        }

        return builder.build(true).toUri();
    }

    public List<Item> searchAllFestivals(String eventStartDate) {

        List<Item> allItems = new ArrayList<>();

        int pageNo = 1;

        while (true) {
            URI uri = buildFestivalUri("/searchFestival2", eventStartDate, pageNo);
            FestivalListResponse festival = getWithRetry(uri, FestivalListResponse.class);

            Body body = (festival.response() == null) ? null : festival.response().body();
            List<Item> items = (body == null || body.items() == null) ? null : body.items().item();

            if (items == null || items.isEmpty()) {
                break;
            }

            allItems.addAll(items);

            if (allItems.size() >= body.totalCount()) {
                break;
            }

            pageNo++;
        }

        return allItems;
    }

    private URI buildFestivalUri(String path, String eventStartDate, int pageNo) {

        if (!StringUtils.hasText(properties.serviceKey())) {
            throw new TourApiException("[tour-api] serviceKey가 비어 있다. TOUR_API_SERVICE_KEY(tour-api.service-key)를 설정하라.");
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(properties.baseUrl())
                .path(path)
                .queryParam("serviceKey", properties.serviceKey())
                .queryParam("MobileOS", "WEB")
                .queryParam("MobileApp", "ProjectY")
                .queryParam("numOfRows", 100)
                .queryParam("eventStartDate", eventStartDate)
                .queryParam("pageNo", pageNo)
                .queryParam("_type", "json");

        return builder.build(true).toUri();
    }

    public FestivalDetailCommonResponse fetchDetailCommon(String contentId) {
        URI uri = buildDetailUri("/detailCommon2", contentId, null);

        return getWithRetry(uri, FestivalDetailCommonResponse.class);
    }

    public FestivalDetailIntroResponse fetchDetailIntro(String contentId) {
        URI uri = buildDetailUri("/detailIntro2", contentId, "15");
        return getWithRetry(uri, FestivalDetailIntroResponse.class);
    }

    private URI buildDetailUri(String path, String contentId, String contentTypeId) {

        if (!StringUtils.hasText(properties.serviceKey())) {
            throw new TourApiException("[tour-api] serviceKey가 비어 있다. TOUR_API_SERVICE_KEY(tour-api.service-key)를 설정하라.");
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(properties.baseUrl())
                .path(path)
                .queryParam("serviceKey", properties.serviceKey())
                .queryParam("MobileOS", "WEB")
                .queryParam("MobileApp", "ProjectY")
                .queryParam("_type", "json")
                .queryParam("contentId", contentId);

        if (StringUtils.hasText(contentTypeId)) {
            builder.queryParam("contentTypeId", contentTypeId);
        }

        return builder.build(true).toUri();
    }

    private <T extends TourApiResponse> T getWithRetry(URI uri, Class<T> responseType) {

        RuntimeException last = null;

        for (int attempt = 1; attempt <= properties.maxAttempts(); attempt++) {
            try {
                T body = client.get()
                        .uri(uri)
                        .accept(MediaType.APPLICATION_JSON, MediaType.ALL)
                        .retrieve()
                        .body(responseType);

                validate(body, uri);
                return body;

            } catch (TourApiException e) {
                throw e;
            } catch (RuntimeException e) {
                last = e;
                log.warn("[tour-api] 호출 실패 attempt={}/{}, url={}, reason={}",
                        attempt, properties.maxAttempts(), maskServiceKey(uri), e.getMessage());
                sleepBeforeRetry(attempt);
            }
        }

        throw new TourApiException("[tour-api] 호출 실패 url=" + maskServiceKey(uri), last);
    }

    private void validate(TourApiResponse body, URI uri) {

        if (body == null || !StringUtils.hasText(body.resultCode())) {
            throw new IllegalStateException("[tour-api] 빈 응답 url=" + maskServiceKey(uri));
        }

        if (!"0000".equals(body.resultCode())) {
            throw new TourApiException(
                    "[tour-api] 실패 응답 resultCode=" + body.resultCode() + ", url=" + maskServiceKey(uri));
        }
    }

    private void sleepBeforeRetry(int attempt) {

        if (properties.retryInitialDelayMs() <= 0 || attempt >= properties.maxAttempts()) {
            return;
        }

        long delayMs = backoffDelayMs(attempt, properties.retryInitialDelayMs(),
                properties.retryMaxDelayMs(), properties.retryBackoffMultiplier());

        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new TourApiException("[tour-api] 재시도 대기 중 인터럽트", exception);
        }
    }

    private long backoffDelayMs(int attempt, long initialDelayMs, long maxDelayMs, double multiplier) {

        if (initialDelayMs <= 0) {
            return 0;
        }

        long safeMaxDelayMs = Math.max(0, maxDelayMs);
        int exponent = Math.max(0, attempt - 1);
        double base = Math.max(1.0d, multiplier);
        double delay = initialDelayMs * Math.pow(base, exponent);

        if (Double.isNaN(delay) || delay >= safeMaxDelayMs) {
            return safeMaxDelayMs;
        }

        long result = (long) delay;
        return Math.min(safeMaxDelayMs, result);
    }

    private String maskServiceKey(URI uri) {
        String uriString = String.valueOf(uri);

        if (!StringUtils.hasText(properties.serviceKey())) {
            return uriString;
        }

        String masked = uriString.replace(properties.serviceKey(), "****");
        String encodedKey = UriUtils.encode(properties.serviceKey(), StandardCharsets.UTF_8);
        return masked.replace(encodedKey, "****");
    }
}
