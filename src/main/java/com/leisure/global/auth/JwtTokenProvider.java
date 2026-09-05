package com.leisure.global.auth;

import com.leisure.global.auth.properties.JwtProperties;
import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.member.domain.Member;
import com.leisure.member.domain.MemberRole;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Date;

@Component
@EnableConfigurationProperties(JwtProperties.class)
public class JwtTokenProvider {

    private final JwtProperties properties;

    private final SecretKey key;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
    }

    public String issueAccessToken(String publicId, String email, MemberRole role, long tokenInvalidationVersion) {
        return issueToken(publicId, email, role, tokenInvalidationVersion, properties.accessTokenExpiration());
    }

    public String issueRefreshToken(String publicId, String email, MemberRole role, long tokenInvalidationVersion) {
        return issueToken(publicId, email, role, tokenInvalidationVersion, properties.refreshTokenExpiration());
    }

    private String issueToken(String publicId, String email, MemberRole role, long tokenInvalidationVersion, long expirationTime) {

        ZonedDateTime issuedAt = ZonedDateTime.now();
        ZonedDateTime expiresAt = issuedAt.plus(Duration.ofMillis(expirationTime));

        return Jwts.builder()
                .subject(publicId)
                .claim("email", email)
                .claim("role", role.name())
                .claim("tokenInvalidationVersion", tokenInvalidationVersion)
                .issuedAt(Date.from(issuedAt.toInstant()))
                .expiration(Date.from(expiresAt.toInstant()))
                .signWith(key)
                .compact();
    }

    public void verifyRefreshToken(String refreshToken) {

        try {
            extractClaims(refreshToken);
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.TOKEN_EXPIRED) {
                throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED);
            }
            throw e;
        }
    }

    private Claims extractClaims(String token) {

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims;

        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        } catch (UnsupportedJwtException e) {
            throw new BusinessException(ErrorCode.TOKEN_UNSUPPORTED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
    }

    public String getPublicId(String token) {
        String publicId = extractClaims(token).getSubject();

        if (publicId == null || publicId.isBlank()) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        return publicId;
    }

    public String getEmail(String token) {
        String email = extractClaims(token).get("email", String.class);

        if (email == null || email.isBlank()) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        return email;
    }

    public MemberRole getRole(String token) {
        String role = extractClaims(token).get("role", String.class);

        if (role == null || role.isBlank()) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        try {
            return MemberRole.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
    }

    public long extractInvalidationVersion(String token) {
        Long version = extractClaims(token).get("tokenInvalidationVersion", Long.class);

        if (version == null) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        return version;
    }

    public long getRemainingAccessTokenTtl(String accessToken) {
        Date expiresAt = extractClaims(accessToken).getExpiration();
        long remaining = expiresAt.getTime() - System.currentTimeMillis();
        // 이미 만료됐거나 만료 직전이면 음수가 될 수 있어 0으로 클램핑 (음수 TTL 방지)
        return Math.max(0, remaining);
    }

    public long getRefreshTokenTtl() {
        return properties.refreshTokenExpiration();
    }
}
