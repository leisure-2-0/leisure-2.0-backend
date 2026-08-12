package com.leisure.member.controller;

import com.leisure.global.response.ApiResponse;
import com.leisure.member.dto.request.SignUpRequest;
import com.leisure.member.dto.response.SignUpResponse;
import com.leisure.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService service;

    @PostMapping("/members")
    public ResponseEntity<ApiResponse<SignUpResponse>> signUp(@Valid @RequestBody SignUpRequest request) {

        SignUpResponse response = service.signUp(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다.", response));
    }

    @GetMapping("/users/email/check")
    public ResponseEntity<ApiResponse<Void>> checkEmail(@RequestParam String email) {

        service.checkEmail(email);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("사용 가능한 이메일입니다.", null));
    }

    @GetMapping("/users/nickname/check")
    public ResponseEntity<ApiResponse<Void>> checkNickname(@RequestParam String nickname) {

        service.checkNickname(nickname);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("사용 가능한 이름입니다.", null));
    }
}
