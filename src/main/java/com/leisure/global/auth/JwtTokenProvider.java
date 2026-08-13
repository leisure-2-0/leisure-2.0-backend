package com.leisure.global.auth;

import com.leisure.global.auth.properties.JwtProperties;
import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
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

    public String issueAccessToken(String publicId, String email, long tokenInvalidationVersion) {
        return issueToken(publicId, email, tokenInvalidationVersion, properties.accessTokenExpiration());
    }

    public String issueRefreshToken(String publicId, String email, long tokenInvalidationVersion) {
        return issueToken(publicId, email, tokenInvalidationVersion, properties.refreshTokenExpiration());
    }

    private String issueToken(String publicId, String email, long tokenInvalidationVersion, long expirationTime) {

        ZonedDateTime issuedAt = ZonedDateTime.now();
        ZonedDateTime expiresAt = issuedAt.plus(Duration.ofMillis(expirationTime));

        return Jwts.builder()
                .subject(publicId)
                .claim("email", email)
                .claim("tokenInvalidationVersion", tokenInvalidationVersion)
                .issuedAt(Date.from(issuedAt.toInstant()))
                .expiration(Date.from(expiresAt.toInstant()))
                .signWith(key)
                .compact();
    }

    public TokenStatus verifyRefreshToken(String token) {

        try {
            extractClaims(token);
            return TokenStatus.VALID;
        } catch (BusinessException e) {
            return switch (e.getErrorCode()) {
                case TOKEN_EXPIRED -> TokenStatus.EXPIRED;
                case TOKEN_UNSUPPORTED -> TokenStatus.UNSUPPORTED;
                default -> TokenStatus.INVALID;
            };
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

    public long getTokenInvalidationVersion(String token) {
        Long version = extractClaims(token).get("tokenInvalidationVersion", Long.class);

        if (version == null) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        return version;
    }
}
