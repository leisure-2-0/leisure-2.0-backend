package com.leisure.auth.service;

import com.leisure.auth.dto.request.LoginRequest;
import com.leisure.auth.dto.result.LoginResult;
import com.leisure.global.auth.JwtTokenProvider;
import com.leisure.global.auth.store.RedisRefreshTokenStore;
import com.leisure.global.auth.store.RedisTokenStatusStore;
import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.member.domain.Member;
import com.leisure.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository repository;

    private final PasswordEncoder encoder;

    private final JwtTokenProvider provider;

    private final RedisTokenStatusStore tokenStatusStore;

    private final RedisRefreshTokenStore refreshTokenStore;


    @Transactional(readOnly = true)
    public LoginResult login(LoginRequest request) {

        Member member = repository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (!member.matchesPassword(request.password(), encoder)) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        String publicId = member.getPublicId();
        String email = member.getEmail();
        long invalidationVersion = tokenStatusStore.getInvalidationVersion(publicId);

        String accessToken = provider.issueAccessToken(publicId, email, invalidationVersion);
        String refreshToken = provider.issueRefreshToken(publicId, email, invalidationVersion);

        refreshTokenStore.save(publicId, refreshToken, provider.getRefreshTokenTtl());

        return new LoginResult(accessToken, refreshToken);
    }
}
