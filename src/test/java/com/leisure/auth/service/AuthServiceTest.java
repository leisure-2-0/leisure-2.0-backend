package com.leisure.auth.service;

import com.leisure.auth.dto.request.LoginRequest;
import com.leisure.auth.dto.result.LoginResult;
import com.leisure.auth.dto.result.ReissueResult;
import com.leisure.global.auth.JwtTokenProvider;
import com.leisure.global.auth.TokenRotationResult;
import com.leisure.global.auth.store.RedisBlacklistTokenStore;
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
import static org.mockito.ArgumentMatchers.any;
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

    @Mock
    private RedisBlacklistTokenStore blacklistTokenStore;

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
        given(tokenStatusStore.getCurrentInvalidationVersion(PUBLIC_ID)).willReturn(0L);
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

    // ===== 토큰 재발급 =====

    private static final String REFRESH_TOKEN = "refresh-token";

    /** reissue 성공 경로에서 rotate 직전까지 필요한 공통 스텁 */
    private void givenReissueUntilRotate() {
        given(provider.getPublicId(REFRESH_TOKEN)).willReturn(PUBLIC_ID);
        given(provider.getEmail(REFRESH_TOKEN)).willReturn(EMAIL);
        given(provider.getRefreshTokenTtl()).willReturn(1000L);
        given(tokenStatusStore.getCurrentInvalidationVersion(PUBLIC_ID)).willReturn(0L);
        given(provider.issueAccessToken(PUBLIC_ID, EMAIL, 0L)).willReturn("new-access-token");
        given(provider.issueRefreshToken(PUBLIC_ID, EMAIL, 0L)).willReturn("new-refresh-token");
    }

    @Test
    @DisplayName("refresh 토큰이 null이면 REFRESH_TOKEN_NOT_FOUND 예외를 던진다")
    void reissue_nullToken() {
        assertThatThrownBy(() -> authService.reissue(null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REFRESH_TOKEN_NOT_FOUND);

        verify(refreshTokenStore, never()).rotate(any());
    }

    @Test
    @DisplayName("refresh 토큰이 공백이면 REFRESH_TOKEN_NOT_FOUND 예외를 던진다")
    void reissue_blankToken() {
        assertThatThrownBy(() -> authService.reissue("   "))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REFRESH_TOKEN_NOT_FOUND);

        verify(refreshTokenStore, never()).rotate(any());
    }

    @Test
    @DisplayName("rotate 성공 시 새 access/refresh 토큰을 담은 ReissueResult를 반환한다")
    void reissue_success() {
        // given
        givenReissueUntilRotate();
        given(refreshTokenStore.rotate(any())).willReturn(TokenRotationResult.SUCCESS);

        // when
        ReissueResult result = authService.reissue(REFRESH_TOKEN);

        // then
        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    @DisplayName("rotate 결과가 NOT_FOUND면 REFRESH_TOKEN_NOT_FOUND 예외를 던진다")
    void reissue_rotateNotFound() {
        // given
        givenReissueUntilRotate();
        given(refreshTokenStore.rotate(any())).willReturn(TokenRotationResult.NOT_FOUND);

        // when & then
        assertThatThrownBy(() -> authService.reissue(REFRESH_TOKEN))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REFRESH_TOKEN_NOT_FOUND);

        verify(refreshTokenStore, never()).remove(anyString());
    }

    @Test
    @DisplayName("rotate 결과가 MISMATCHED면 재사용으로 간주해 세션을 무효화하고 REFRESH_TOKEN_REUSE_DETECTED를 던진다")
    void reissue_rotateMismatched() {
        // given
        givenReissueUntilRotate();
        given(refreshTokenStore.rotate(any())).willReturn(TokenRotationResult.MISMATCHED);

        // when & then
        assertThatThrownBy(() -> authService.reissue(REFRESH_TOKEN))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REFRESH_TOKEN_REUSE_DETECTED);

        // 탈취 대응: 저장된 refresh 제거 + 회원 전 세션 무효화
        verify(refreshTokenStore).remove(PUBLIC_ID);
        verify(tokenStatusStore).increaseInvalidationVersion(PUBLIC_ID);
    }

    @Test
    @DisplayName("rotate 결과가 CONCURRENTLY_UPDATED면 재사용으로 간주해 세션을 무효화하고 REFRESH_TOKEN_REUSE_DETECTED를 던진다")
    void reissue_rotateConcurrentlyUpdated() {
        // given
        givenReissueUntilRotate();
        given(refreshTokenStore.rotate(any())).willReturn(TokenRotationResult.CONCURRENTLY_UPDATED);

        // when & then
        assertThatThrownBy(() -> authService.reissue(REFRESH_TOKEN))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REFRESH_TOKEN_REUSE_DETECTED);

        verify(refreshTokenStore).remove(PUBLIC_ID);
        verify(tokenStatusStore).increaseInvalidationVersion(PUBLIC_ID);
    }

    // ===== 로그아웃 =====

    private static final String ACCESS_TOKEN = "access-token";

    @Test
    @DisplayName("access 토큰을 남은 TTL만큼 블랙리스트에 등록하고 refresh 토큰을 제거한다")
    void logout_success() {
        // given
        given(provider.getRemainingAccessTokenTtl(ACCESS_TOKEN)).willReturn(1000L);

        // when
        authService.logout(PUBLIC_ID, ACCESS_TOKEN);

        // then
        verify(blacklistTokenStore).save(ACCESS_TOKEN, 1000L);
        verify(refreshTokenStore).remove(PUBLIC_ID);
    }

    @Test
    @DisplayName("access 토큰이 null이면 TOKEN_INVALID 예외를 던진다")
    void logout_nullToken() {
        assertThatThrownBy(() -> authService.logout(PUBLIC_ID, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOKEN_INVALID);

        verify(blacklistTokenStore, never()).save(anyString(), anyLong());
        verify(refreshTokenStore, never()).remove(anyString());
    }

    @Test
    @DisplayName("access 토큰이 공백이면 TOKEN_INVALID 예외를 던진다")
    void logout_blankToken() {
        assertThatThrownBy(() -> authService.logout(PUBLIC_ID, "   "))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOKEN_INVALID);

        verify(blacklistTokenStore, never()).save(anyString(), anyLong());
        verify(refreshTokenStore, never()).remove(anyString());
    }
}
