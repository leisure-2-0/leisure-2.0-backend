package com.leisure.global.auth;

import com.leisure.global.auth.properties.CookieProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@EnableConfigurationProperties(CookieProperties.class)
@RequiredArgsConstructor
public class CookieProvider {

    private final CookieProperties properties;

    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(properties.name(), refreshToken)
                .path(properties.path())
                .httpOnly(true)
                .secure(properties.secure())
                .sameSite(properties.sameSite())
                .maxAge(properties.maxAge())
                .build();
    }

    public ResponseCookie createClearRefreshTokenCookie() {
        return ResponseCookie.from(properties.name(), "")
                .path(properties.path())
                .httpOnly(true)
                .secure(properties.secure())
                .sameSite(properties.sameSite())
                .maxAge(Duration.ZERO)
                .build();
    }
}
