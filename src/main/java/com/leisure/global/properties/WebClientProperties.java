package com.leisure.global.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("ai.server")
public record WebClientProperties(

        String baseUrl,

        @DefaultValue("5000") int connectTimeoutMs,

        @DefaultValue("60") int readTimeoutSeconds
) {
}
