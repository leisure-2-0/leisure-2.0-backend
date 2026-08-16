package com.leisure.member.service;

import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.global.provider.Provider;
import com.leisure.member.domain.Member;
import com.leisure.member.dto.request.SignUpRequest;
import com.leisure.member.dto.response.SignUpResponse;
import com.leisure.member.event.MemberWithdrawnEvent;
import com.leisure.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository repository;

    private final PasswordEncoder encoder;

    private final Provider provider;

    private final ApplicationEventPublisher eventPublisher;

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

    @Transactional
    public void withdraw(String publicId) {
        Member member = provider.getMemberByPublicId(publicId);

        if (member.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        member.delete();
        eventPublisher.publishEvent(new MemberWithdrawnEvent(publicId));
    }

    @Transactional(readOnly = true)
    public void checkEmail(String email) {
        if (repository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATE);
        }
    }

    @Transactional(readOnly = true)
    public void checkNickname(String nickname) {
        if (repository.existsByNickname(nickname)) {
            throw new BusinessException(ErrorCode.NICKNAME_DUPLICATE);
        }
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

