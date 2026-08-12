package com.leisure.member.service;

import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.member.domain.Member;
import com.leisure.member.dto.request.SignUpRequest;
import com.leisure.member.dto.response.SignUpResponse;
import com.leisure.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository repository;

    private final PasswordEncoder encoder;

    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {

        validatePasswordMatch(request.password(), request.passwordCheck());

        validateMemberUniqueness(request.email(), request.nickname());

        // TODO: PreSigned URL 기반 이미지 업로드 로직

        String encodedPassword = encoder.encode(request.password());

        Member member = Member.create(
                request.email(),
                encodedPassword,
                request.nickname(),
                request.profileImageUrl());

        try {
            repository.save(member);
            repository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATE);
        }

        return new SignUpResponse(member.getPublicId());
    }

    private void validateMemberUniqueness(String email, String nickname) {
        if (repository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATE);
        }

        if (repository.existsByNickname(nickname)) {
            throw new BusinessException(ErrorCode.NICKNAME_DUPLICATE);
        }
    }

    private void validatePasswordMatch(String password, String passwordCheck) {
        if (!password.equals(passwordCheck)) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }
    }
}

