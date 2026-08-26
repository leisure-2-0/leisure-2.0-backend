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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "회원(Member)",
        description = "회원가입, 회원탈퇴, 특정 회원 조회, 프로필 수정, 비밀번호 변경, 이메일·닉네임 중복 검증"
)
@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService service;

    private final CookieProvider provider;

    @Operation(
                summary = "회원 가입",
                description = "이메일·비밀번호·닉네임·프로필 이미지로 회원을 생성합니다. 이메일은 소문자로 정규화되어 저장됩니다."
    )
    @PostMapping("/members")
    public ResponseEntity<ApiResponse<SignUpResponse>> signUp(@Valid @RequestBody SignUpRequest request) {

        SignUpResponse response = service.signUp(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다.", response));
    }

    @Operation(
                summary = "회원 탈퇴",
                description = "현재 로그인한 회원을 소프트 삭제하고 refresh 토큰 쿠키를 제거합니다.")
    @SecurityRequirement(name = "BearerAuth")
    @DeleteMapping("/members")
    public ResponseEntity<Void> withdraw(@CurrentMember String publicId, HttpServletResponse response) {

        service.withdraw(publicId);

        ResponseCookie cookie = provider.createClearRefreshTokenCookie();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.noContent().build();
    }

    @Operation(
                summary = "내 프로필 조회",
                description = "현재 로그인한 회원의 이메일·닉네임·프로필 이미지를 조회한다."
    )
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/members/me")
    public ResponseEntity<ApiResponse<MemberProfileResponse>> getMyProfile(@CurrentMember String publicId) {

        MemberProfileResponse response = service.getMyProfile(publicId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response.nickname() + "님의 회원 정보 조회에 성공했습니다.", response));

    }

    @Operation(
                summary = "프로필 수정",
                description = "닉네임·프로필 이미지를 수정한다. null 필드는 유지되며, 닉네임 변경 시 중복을 검사한다."
    )
    @SecurityRequirement(name = "BearerAuth")
    @PatchMapping("/members/me")
    public ResponseEntity<ApiResponse<ProfileChangeResponse>> changeProfile(@CurrentMember String publicId, @Valid @RequestBody ProfileChangeRequest request) {

        ProfileChangeResponse response = service.changeProfile(publicId, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response.nickname() + "님의 프로필이 수정되었습니다.", response));
    }

    @Operation(
                summary = "비밀번호 변경",
                description = "현재 비밀번호 확인 후 변경한다. 다른 기기·세션은 모두 로그아웃되고 요청한 세션만 새 토큰으로 유지된다."
    )
    @PatchMapping("/members/me/password")
    @SecurityRequirement(name = "BearerAuth")
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

    @Operation(
                summary = "이메일 중복 확인",
                description = "가입 가능한 이메일인지 확인한다. 이미 사용 중이면 409를 반환한다."
    )
    @GetMapping("/members/email/check")
    public ResponseEntity<ApiResponse<Void>> checkEmail(@RequestParam String email) {

        service.checkEmail(email);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("사용 가능한 이메일입니다.", null));
    }

    @Operation(
                summary = "닉네임 중복 확인",
                description = "사용 가능한 닉네임인지 확인한다. 이미 사용 중이면 409를 반환한다."
    )
    @GetMapping("/members/nickname/check")
    public ResponseEntity<ApiResponse<Void>> checkNickname(@RequestParam String nickname) {

        service.checkNickname(nickname);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("사용 가능한 이름입니다.", null));
    }
}
