package com.leisure.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfiguration {


    @Bean
    public RestClient restClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

        // 연결(TCP 핸드셰이크) 대기 한도 3초
        // 이 안에 연결 못 맺으면 서버 다운/네트워크 문제로 보고 빠르게 실패
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));

        // 연결 후 응답 데이터 수신 대기 한도 10초
        // 공공 API가 느릴 수 있어 여유. 초과 시 끊어 스레드 무한 대기 방지
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}
