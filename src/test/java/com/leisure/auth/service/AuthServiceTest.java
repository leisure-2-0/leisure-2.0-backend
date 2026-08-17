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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MemberRepository repository;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private JwtTokenProvider provider;

    @Mock
    private RedisTokenStatusStore tokenStatusStore;

    @Mock
    private RedisRefreshTokenStore refreshTokenStore;

    @InjectMocks
    private AuthService authService;

    private static final String EMAIL = "user@leisure.com";
    private static final String RAW_PASSWORD = "Passw0rd!";
    private static final String ENCODED_PASSWORD = "ENCODED_PASSWORD";
    private static final String PUBLIC_ID = "public-id";

    private Member member;

    @BeforeEach
    void setUp() {
        // 저장된 비밀번호는 인코딩된 값, publicId는 @PrePersist 대신 리플렉션으로 주입
        member = Member.create(EMAIL, ENCODED_PASSWORD, "nickname", null);
        ReflectionTestUtils.setField(member, "publicId", PUBLIC_ID);
    }

    private LoginRequest request(String email, String password) {
        return new LoginRequest(email, password);
    }

    @Test
    @DisplayName("정상 자격 증명이면 access/refresh 토큰을 발급하고 refresh 토큰을 저장한다")
    void login_success() {
        // given
        LoginRequest request = request(EMAIL, RAW_PASSWORD);
        given(repository.findByEmail(EMAIL)).willReturn(Optional.of(member));
        given(encoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).willReturn(true);
        given(tokenStatusStore.getInvalidationVersion(PUBLIC_ID)).willReturn(0L);
        given(provider.issueAccessToken(PUBLIC_ID, EMAIL, 0L)).willReturn("access-token");
        given(provider.issueRefreshToken(PUBLIC_ID, EMAIL, 0L)).willReturn("refresh-token");
        given(provider.getRefreshTokenTtl()).willReturn(1000L);

        // when
        LoginResult result = authService.login(request);

        // then
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        verify(refreshTokenStore).save(PUBLIC_ID, "refresh-token", 1000L);
    }

    @Test
    @DisplayName("이메일에 해당하는 회원이 없으면 LOGIN_FAILED 예외를 던진다")
    void login_emailNotFound() {
        // given
        LoginRequest request = request(EMAIL, RAW_PASSWORD);
        given(repository.findByEmail(EMAIL)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LOGIN_FAILED);

        verify(provider, never()).issueAccessToken(anyString(), anyString(), anyLong());
        verify(refreshTokenStore, never()).save(anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 LOGIN_FAILED 예외를 던진다")
    void login_passwordMismatch() {
        // given
        LoginRequest request = request(EMAIL, RAW_PASSWORD);
        given(repository.findByEmail(EMAIL)).willReturn(Optional.of(member));
        given(encoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LOGIN_FAILED);

        verify(provider, never()).issueAccessToken(anyString(), anyString(), anyLong());
        verify(refreshTokenStore, never()).save(anyString(), anyString(), anyLong());
    }
}
