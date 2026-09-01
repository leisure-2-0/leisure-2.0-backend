package com.leisure.global.external.tourapi;

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

    private static final String SUCCESS_CODE = "0000";

    private final RestClient restClient;

    private final TourApiProperties properties;

    public TourApiClient(RestClient restClient, TourApiProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    // 광역(시/도) 법정동 코드 목록 — lDongRegnCd 파라미터 없이 호출
    public LdongCodeResponse fetchRegions() {
        URI uri = buildUri("/ldongCode2", null);
        return getWithRetry(uri, LdongCodeResponse.class);
    }

    // 특정 광역(lDongRegnCd)에 속한 시군구 법정동 코드 목록
    public LdongCodeResponse fetchSigungus(String lDongRegnCd) {
        URI uri = buildUri("/ldongCode2", lDongRegnCd);
        return getWithRetry(uri, LdongCodeResponse.class);
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

    // lDongRegnCd가 null이면 광역 목록, 값이 있으면 그 광역의 시군구 목록을 요청하는 URI를 만든다.
    private URI buildUri(String path, String lDongRegnCd) {

        // null/빈/공백 키면 호출 자체를 막는다 (hasText가 셋 다 잡아줌 — trim/NPE 불필요)
        if (!StringUtils.hasText(properties.serviceKey())) {
            throw new TourApiException("[tour-api] serviceKey가 비어 있다. TOUR_API_SERVICE_KEY(tour-api.service-key)를 설정하라.");
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(properties.baseUrl())
                .path(path)
                .queryParam("serviceKey", properties.serviceKey()) // 이미 인코딩된(Encoding) 키
                .queryParam("MobileOS", "WEB")
                .queryParam("MobileApp", "ProjectY")
                .queryParam("numOfRows", 100)
                .queryParam("pageNo", 1)
                .queryParam("_type", "json");

        // 광역 코드가 있으면 시군구 조회, 없으면 광역 목록 조회
        if (StringUtils.hasText(lDongRegnCd)) {
            builder.queryParam("lDongRegnCd", lDongRegnCd);
        }

        // build(true): serviceKey가 이미 인코딩돼 있어 재인코딩(이중 인코딩)을 막는다
        return builder.build(true).toUri();
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

    // 실패 시 재시도(지수 백오프). 공공 API는 간헐적 실패가 잦아 재시도로 흡수한다.
    private <T extends TourApiResponse> T getWithRetry(URI uri, Class<T> responseType) {

        RuntimeException last = null;

        for (int attempt = 1; attempt <= properties.maxAttempts(); attempt++) {
            try {
                T body = restClient.get()
                        .uri(uri)
                        .accept(MediaType.APPLICATION_JSON, MediaType.ALL)
                        .retrieve()
                        .body(responseType);

                validate(body, uri);   // 200이지만 빈 응답/에러 코드면 예외
                return body;

            } catch (TourApiException e) {
                throw e;               // 검증 실패(인증만료·트래픽초과 등)는 재시도해도 소용없어 즉시 던짐
            } catch (RuntimeException e) {
                last = e;              // 네트워크 등만 재시도 대상
                log.warn("[tour-api] 호출 실패 attempt={}/{}, url={}, reason={}",
                        attempt, properties.maxAttempts(), maskServiceKey(uri), e.getMessage());
                sleepBeforeRetry(attempt);
            }
        }

        // 모든 시도 실패 후에만 최종 던진다 (throw는 for 밖)
        throw new TourApiException("[tour-api] 호출 실패 url=" + maskServiceKey(uri), last);
    }

    // 200 응답이어도 body가 비었거나 resultCode가 성공(0000)이 아니면 예외
    private void validate(TourApiResponse body, URI uri) {
        if (body == null) {
            throw new TourApiException("[tour-api] 빈 응답 url=" + maskServiceKey(uri));
        }
        if (!SUCCESS_CODE.equals(body.resultCode())) {
            throw new TourApiException(
                    "[tour-api] 실패 응답 resultCode=" + body.resultCode() + ", url=" + maskServiceKey(uri));
        }
    }

    // 재시도 간 대기. 마지막 시도 뒤엔 기다릴 필요 없어 바로 반환.
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

    // attempt에 따라 initial * multiplier^(attempt-1)로 늘리되 max를 넘지 않게 클램핑
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

    // 로그에 serviceKey가 노출되지 않도록 원본/인코딩 키를 모두 마스킹한다.
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
