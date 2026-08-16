package com.leisure.global.auth;

import com.leisure.global.auth.principal.MemberPrincipal;
import com.leisure.global.auth.resolver.AccessTokenResolver;
import com.leisure.global.auth.store.RedisBlacklistTokenStore;
import com.leisure.global.auth.store.RedisTokenStatusStore;
import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider provider;

    private final AccessTokenResolver resolver;

    private final RedisBlacklistTokenStore blacklistTokenStore;

    private final RedisTokenStatusStore tokenStatusStore;

    private final SecurityErrorResponseWriter writer;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        try {
            String pureToken = resolver.resolve(request);

            if (pureToken == null) {
                filterChain.doFilter(request, response);
                return;
            }


            if (blacklistTokenStore.exists(pureToken)) {
                writer.write(response, ErrorCode.TOKEN_BLACKLISTED);
                return;
            }

            String publicId = provider.getPublicId(pureToken);
            String email = provider.getEmail(pureToken);

            long claimVersion = provider.getTokenInvalidationVersion(pureToken);
            long storedVersion = tokenStatusStore.getInvalidationVersion(publicId);

            if (claimVersion != storedVersion) {
                writer.write(response, ErrorCode.TOKEN_INVALID);
                return;
            }

            MemberPrincipal principal = new MemberPrincipal(publicId, email);

            Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());

            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);
        } catch (BusinessException e) {
            SecurityContextHolder.clearContext();
            writer.write(response, e.getErrorCode());
        }
    }
}


