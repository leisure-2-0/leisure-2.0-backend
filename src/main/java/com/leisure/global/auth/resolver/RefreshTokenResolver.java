package com.leisure.global.auth.resolver;

import com.leisure.global.auth.properties.CookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class RefreshTokenResolver {

    private final CookieProperties properties;

    public String resolve(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        return Arrays.stream(cookies)
                .filter(cookie -> properties.name().equals(cookie.getName()))
                .map(cookie -> cookie.getValue())
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }
}
