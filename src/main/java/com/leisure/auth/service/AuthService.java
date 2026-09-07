package com.leisure.auth.service;

import com.leisure.auth.dto.request.LoginRequest;
import com.leisure.auth.dto.result.LoginResult;
import com.leisure.auth.dto.result.ReissueResult;
import com.leisure.global.auth.JwtTokenProvider;
import com.leisure.global.auth.TokenRotationContext;
import com.leisure.global.auth.store.RedisBlacklistTokenStore;
import com.leisure.global.auth.store.RedisRefreshTokenStore;
import com.leisure.global.auth.store.RedisTokenStatusStore;
import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.member.domain.Member;
import com.leisure.member.domain.MemberRole;
import com.leisure.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtTokenProvider jwtTokenProvider;

    private final RedisTokenStatusStore tokenStatusStore;

    private final RedisRefreshTokenStore refreshTokenStore;

    private final RedisBlacklistTokenStore blacklistTokenStore;


    @Transactional(readOnly = true)
    public LoginResult login(LoginRequest request) {

        Member member = memberRepository.findByEmailAndDeletedAtIsNull(Member.normalizeEmail(request.email()))
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (!member.matchesPassword(request.password(), passwordEncoder)) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        String publicId = member.getPublicId();
        String email = member.getEmail();
        MemberRole role = member.getRole();
        long ttl = jwtTokenProvider.getRefreshTokenTtl();
        long invalidationVersion = tokenStatusStore.getCurrentInvalidationVersion(publicId);

        String accessToken = jwtTokenProvider.issueAccessToken(publicId, email, role, invalidationVersion);
        String refreshToken = jwtTokenProvider.issueRefreshToken(publicId, email, role, invalidationVersion);

        refreshTokenStore.save(publicId, refreshToken, ttl);

        return new LoginResult(accessToken, refreshToken);
    }


    public void logout(String publicId, String accessToken) {

        if (accessToken == null || accessToken.isBlank()) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        long ttl = jwtTokenProvider.getRemainingAccessTokenTtl(accessToken);

        blacklistTokenStore.save(accessToken, ttl);
        refreshTokenStore.remove(publicId);
    }

    public ReissueResult reissue(String refreshToken) {

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        jwtTokenProvider.verifyRefreshToken(refreshToken);
        String publicId = jwtTokenProvider.getPublicId(refreshToken);
        String email = jwtTokenProvider.getEmail(refreshToken);
        MemberRole role = jwtTokenProvider.getRole(refreshToken);
        long ttl = jwtTokenProvider.getRefreshTokenTtl();

        long invalidationVersion = tokenStatusStore.getCurrentInvalidationVersion(publicId);

        String newAccessToken = jwtTokenProvider.issueAccessToken(publicId, email, role, invalidationVersion);
        String newRefreshToken = jwtTokenProvider.issueRefreshToken(publicId, email, role, invalidationVersion);

        TokenRotationContext context = new TokenRotationContext(publicId, refreshToken, newRefreshToken, ttl);

        return switch (refreshTokenStore.rotate(context)) {
            case NOT_FOUND -> throw new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
            case MISMATCHED, CONCURRENTLY_UPDATED -> {
                refreshTokenStore.remove(publicId);
                tokenStatusStore.increaseInvalidationVersion(publicId);
                throw new BusinessException(ErrorCode.REFRESH_TOKEN_REUSE_DETECTED);
            }
            case SUCCESS -> new ReissueResult(newAccessToken, newRefreshToken);
        };
    }
}
