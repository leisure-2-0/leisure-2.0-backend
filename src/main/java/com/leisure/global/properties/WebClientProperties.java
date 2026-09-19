package com.leisure.global.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("ai.server")
public record WebClientProperties(String baseUrl, int connectTimeoutMs, int readTimeoutSeconds) {
}
