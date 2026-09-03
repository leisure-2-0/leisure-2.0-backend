package com.leisure.global.external.tourapi;

import com.leisure.global.external.tourapi.dto.response.LdongCodeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("TourAPI 재시도 정책 (TourApiClient.getWithRetry)")
class TourApiClientTest {

    private static final int MAX_ATTEMPTS = 3;

    private static final String OK_BODY = """
            {"response":{"header":{"resultCode":"0000","resultMsg":"OK"},
             "body":{"items":{"item":[{"code":"51","name":"강원특별자치도"}]}}}}""";

    private MockRestServiceServer server;

    private TourApiClient tourApiClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        // retryInitialDelayMs=0 → sleep 없이 빠르게 재시도
        TourApiProperties properties = new TourApiProperties(
                "http://tour-api.test", "test-key", MAX_ATTEMPTS, 0L, 0L, 2.0);

        tourApiClient = new TourApiClient(restClient, properties);
    }

    @Test
    @DisplayName("정상 응답이면 결과를 반환하고 재시도하지 않는다")
    void success() {
        server.expect(times(1), requestTo(containsString("/ldongCode2")))
                .andRespond(withSuccess(OK_BODY, MediaType.APPLICATION_JSON));

        LdongCodeResponse response = tourApiClient.fetchRegions();

        assertThat(response.resultCode()).isEqualTo("0000");
        server.verify();
    }

    @Test
    @DisplayName("4xx(429)는 재시도 없이 즉시 실패한다")
    void clientError_noRetry() {
        server.expect(times(1), requestTo(containsString("/ldongCode2")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> tourApiClient.fetchRegions())
                .isInstanceOf(TourApiException.class);
        server.verify();   // 정확히 1회 = 재시도 안 함
    }

    @Test
    @DisplayName("resultCode 오류 응답(200)은 재시도 없이 즉시 실패한다")
    void resultCodeError_noRetry() {
        String errorBody = """
                {"response":{"header":{"resultCode":"9999","resultMsg":"ERROR"}}}""";
        server.expect(times(1), requestTo(containsString("/ldongCode2")))
                .andRespond(withSuccess(errorBody, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> tourApiClient.fetchRegions())
                .isInstanceOf(TourApiException.class);
        server.verify();
    }

    @Test
    @DisplayName("5xx는 maxAttempts만큼 재시도한 뒤 실패한다")
    void serverError_retriesThenFails() {
        server.expect(times(MAX_ATTEMPTS), requestTo(containsString("/ldongCode2")))
                .andRespond(withServerError());

        assertThatThrownBy(() -> tourApiClient.fetchRegions())
                .isInstanceOf(TourApiException.class);
        server.verify();   // MAX_ATTEMPTS회 호출 = 재시도함
    }
}
