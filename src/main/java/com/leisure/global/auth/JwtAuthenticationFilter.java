package com.leisure.global.auth;

import com.leisure.global.auth.resolver.AccessTokenResolver;
import com.leisure.global.auth.store.RedisBlacklistTokenStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider provider;

    private final AccessTokenResolver resolver;

    private final RedisBlacklistTokenStore blacklistTokenStore;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String pureToken = resolver.resolve(request);

        if (pureToken == null) {
            filterChain.doFilter(request, response);
            return;
        }


        if (blacklistTokenStore.exists(pureToken)) {
            return;
        }

    }
}
