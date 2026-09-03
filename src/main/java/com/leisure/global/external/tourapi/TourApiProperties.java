package com.leisure.global.external.tourapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("tour-api")
public record TourApiProperties(
        String baseUrl,

        String serviceKey,

        int maxAttempts, // 총 시도 횟수. 3 이면 최초 호출 1 회 + 재시도 2 회

        long retryInitialDelayMs, // 첫 번재 실패 후 기다릴 기본 대기 시간(ms)

        long retryMaxDelayMs, // 지수 백오프로 늘어난 대기 시간이 넘지 못하는 최대 대기 시간(ms)

        double retryBackoffMultiplier // 실패가 반복될 때 대기 시간을 몇 배씩 늘릴지 정하는 배수
) {}
