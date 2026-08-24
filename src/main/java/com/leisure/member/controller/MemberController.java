package com.leisure.member.controller;

import com.leisure.auth.dto.response.ReissueResponse;
import com.leisure.auth.dto.result.ReissueResult;
import com.leisure.global.auth.CookieProvider;
import com.leisure.global.auth.CurrentMember;
import com.leisure.global.response.ApiResponse;
import com.leisure.member.dto.request.PasswordChangeRequest;
import com.leisure.member.dto.request.ProfileChangeRequest;
import com.leisure.member.dto.request.SignUpRequest;
import com.leisure.member.dto.response.MemberProfileResponse;
import com.leisure.member.dto.response.ProfileChangeResponse;
import com.leisure.member.dto.response.SignUpResponse;
import com.leisure.member.service.MemberService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService service;

    private final CookieProvider provider;

    @PostMapping("/members")
    public ResponseEntity<ApiResponse<SignUpResponse>> signUp(@Valid @RequestBody SignUpRequest request) {

        SignUpResponse response = service.signUp(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다.", response));
    }

    @DeleteMapping("/members")
    public ResponseEntity<Void> withdraw(@CurrentMember String publicId, HttpServletResponse response) {

        service.withdraw(publicId);

        ResponseCookie cookie = provider.createClearRefreshTokenCookie();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/members/me")
    public ResponseEntity<ApiResponse<MemberProfileResponse>> getMyProfile(@CurrentMember String publicId) {

        MemberProfileResponse response = service.getMyProfile(publicId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response.nickname() + "님의 회원 정보 조회에 성공했습니다.", response));

    }

    @PatchMapping("/members/me")
    public ResponseEntity<ApiResponse<ProfileChangeResponse>> changeProfile(@CurrentMember String publicId, @Valid @RequestBody ProfileChangeRequest request) {

        ProfileChangeResponse response = service.changeProfile(publicId, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response.nickname() + "님의 프로필이 수정되었습니다.", response));
    }

    @PatchMapping("/members/me/password")
    public ResponseEntity<ApiResponse<ReissueResponse>> changePassword(
            @CurrentMember String publicId,
            @Valid @RequestBody PasswordChangeRequest request,
            HttpServletResponse response) {

        ReissueResult result = service.changePassword(publicId, request);

        ResponseCookie refreshInCookie = provider.createRefreshTokenCookie(result.refreshToken());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshInCookie.toString());

        ReissueResponse reissueResponse = new ReissueResponse(result.accessToken());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("비밀번호가 변경되었습니다.", reissueResponse));
    }

    @GetMapping("/members/email/check")
    public ResponseEntity<ApiResponse<Void>> checkEmail(@RequestParam String email) {

        service.checkEmail(email);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("사용 가능한 이메일입니다.", null));
    }

    @GetMapping("/members/nickname/check")
    public ResponseEntity<ApiResponse<Void>> checkNickname(@RequestParam String nickname) {

        service.checkNickname(nickname);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("사용 가능한 이름입니다.", null));
    }
}
