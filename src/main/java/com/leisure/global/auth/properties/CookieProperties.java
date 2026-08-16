package com.leisure.global.auth.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cookie.refresh-token")
public record CookieProperties(String name, String path, boolean secure, String sameSite, long maxAge) {
}
