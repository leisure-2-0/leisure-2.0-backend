package com.leisure.auth.controller;

import com.leisure.auth.dto.request.LoginRequest;
import com.leisure.auth.dto.response.LoginResponse;
import com.leisure.auth.dto.response.ReissueResponse;
import com.leisure.auth.dto.result.LoginResult;
import com.leisure.auth.dto.result.ReissueResult;
import com.leisure.auth.service.AuthService;
import com.leisure.global.auth.CookieProvider;
import com.leisure.global.auth.CurrentMember;
import com.leisure.global.auth.resolver.AccessTokenResolver;
import com.leisure.global.auth.resolver.RefreshTokenResolver;
import com.leisure.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "인증(Auth)",
        description = "로그인, 로그아웃, 토큰 재발급"
)
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    private final CookieProvider provider;

    private final AccessTokenResolver accessResolver;

    private final RefreshTokenResolver refreshResolver;

    @Operation(summary = "로그인", description = "이메일, 비밀번호로 로그인한다. access 토큰은 body로, refresh 토큰은 쿠키로 반환된다.")
    @PostMapping("/auth")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        LoginResult result = service.login(request);

        ResponseCookie refreshInCookie = provider.createRefreshTokenCookie(result.refreshToken());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshInCookie.toString());

        LoginResponse loginResponse = new LoginResponse(result.accessToken());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("로그인에 성공했습니다.", loginResponse));
    }

    @Operation(summary = "로그아웃", description = "access 토큰을 블랙리스트에 등록하고 refresh 토큰을 제거한다.")
    @SecurityRequirement(name = "BearerAuth")
    @DeleteMapping("/auth")
    public ResponseEntity<ApiResponse<Void>> logout(@CurrentMember String publicId, HttpServletRequest request, HttpServletResponse response)  {

        String accessToken = accessResolver.resolve(request);
        service.logout(publicId, accessToken);

        ResponseCookie clearedCookie = provider.createClearRefreshTokenCookie();
        response.addHeader(HttpHeaders.SET_COOKIE, clearedCookie.toString());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "토큰 재발급", description = "쿠키의 refresh 토큰으로 access, refresh 토큰을 재발급한다(rotation). Bearer 토큰 불필요.")
    @PostMapping("/auth/refresh")
    public ResponseEntity<ApiResponse<ReissueResponse>> reissue(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = refreshResolver.resolve(request);

        ReissueResult result = service.reissue(refreshToken);

        ResponseCookie refreshInCookie = provider.createRefreshTokenCookie(result.refreshToken());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshInCookie.toString());

        ReissueResponse reissueResponse = new ReissueResponse(result.accessToken());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("토큰이 재발급되었습니다", reissueResponse));
    }
 }
