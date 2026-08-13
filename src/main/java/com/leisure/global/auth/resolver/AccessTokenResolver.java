package com.leisure.global.auth.resolver;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class AccessTokenResolver {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private static final String BEARER_PREFIX = "Bearer ";

    public String resolve(HttpServletRequest request) {

        String authorization = request.getHeader(AUTHORIZATION_HEADER);

        if (authorization == null || authorization.isBlank() || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }

        // 1. replace 방식
        // "Bearer "라는 글자를 찾아서 빈 값("")으로 바꿈
        // 시간 복잡도: O(N)
        // String pureToken = authorization.replace("Bearer ", "");

        // 2. substring 방식
        // 7번째 인덱스부터 끝까지 잘라냄 ("Bearer " 가 7글자니까 7번부터가 토큰)
        // 시간 복잡도: O(1) ~ O(K) (K: 토큰 길이)
        String pureToken = authorization.substring(BEARER_PREFIX.length());

        return pureToken.isBlank() ? null : pureToken;
    }
}
