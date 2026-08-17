package com.leisure.auth.controller;

import com.leisure.auth.dto.request.LoginRequest;
import com.leisure.auth.dto.response.LoginResponse;
import com.leisure.auth.dto.result.LoginResult;
import com.leisure.auth.service.AuthService;
import com.leisure.global.auth.CookieProvider;
import com.leisure.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    private final CookieProvider provider;

    @PostMapping("/auth")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        LoginResult result = service.login(request);

        ResponseCookie refreshCookie = provider.createRefreshTokenCookie(result.refreshToken());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        LoginResponse loginResponse = new LoginResponse(result.accessToken());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("로그인에 성공했습니다.", loginResponse));
    }
}
