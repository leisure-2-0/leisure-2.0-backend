package com.leisure.member.service;

import com.leisure.auth.dto.result.ReissueResult;
import com.leisure.global.auth.JwtTokenProvider;
import com.leisure.global.auth.store.RedisRefreshTokenStore;
import com.leisure.global.auth.store.RedisTokenStatusStore;
import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.member.domain.Member;
import com.leisure.member.dto.request.PasswordChangeRequest;
import com.leisure.member.dto.request.ProfileChangeRequest;
import com.leisure.member.dto.request.SignUpRequest;
import com.leisure.member.dto.response.MemberProfileResponse;
import com.leisure.member.dto.response.ProfileChangeResponse;
import com.leisure.member.dto.response.SignUpResponse;
import com.leisure.member.event.MemberWithdrawnEvent;
import com.leisure.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository repository;

    private final PasswordEncoder encoder;

    private final MemberReader reader;

    private final ApplicationEventPublisher eventPublisher;

    private final RedisTokenStatusStore tokenStatusStore;

    private final RedisRefreshTokenStore refreshTokenStore;

    private final JwtTokenProvider tokenProvider;

    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {

        validatePasswordMatch(request.password(), request.passwordCheck());

        String email = Member.normalizeEmail(request.email());

        validateMemberUniqueness(email, request.nickname());

        // TODO: PreSigned URL 기반 이미지 업로드 로직

        String encodedPassword = encoder.encode(request.password());

        Member member = Member.create(
                email,
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
        Member member = reader.getMemberByPublicId(publicId);

//        if (member.getDeletedAt() != null) {
//            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
//        }

        member.delete();
        eventPublisher.publishEvent(new MemberWithdrawnEvent(publicId));
    }


    @Transactional(readOnly = true)
    public MemberProfileResponse getMyProfile(String publicId) {
        Member member = reader.getMemberByPublicId(publicId);

        return new MemberProfileResponse(member.getPublicId(), member.getEmail(), member.getNickname(), member.getProfileImageUrl());
    }

    @Transactional
    public ProfileChangeResponse changeProfile(String publicId, ProfileChangeRequest request) {

        Member member = reader.getMemberByPublicId(publicId);

        String nickname = request.nickname();
        String profileImageUrl = request.profileImageUrl();

        if (nickname != null && !Objects.equals(member.getNickname(), nickname)) {
            if (repository.existsByNicknameAndDeletedAtIsNull(nickname)) {
                throw new BusinessException(ErrorCode.NICKNAME_DUPLICATE);
            }
            member.changeNickname(nickname);
        }

        if (profileImageUrl != null) {
            member.changeProfileImageUrl(profileImageUrl);
        }

        return new ProfileChangeResponse(member.getPublicId(), member.getEmail(), member.getNickname(), member.getProfileImageUrl());
    }


    @Transactional
    public ReissueResult changePassword(String publicId, PasswordChangeRequest request) {

        validatePasswordMatch(request.newPassword(), request.newPasswordConfirm());

        Member member = reader.getMemberByPublicId(publicId);

        if (!member.matchesPassword(request.currentPassword(), encoder)) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }

        member.changePassword(encoder.encode(request.newPassword()));

        tokenStatusStore.increaseInvalidationVersion(publicId);
        long invalidationVersion = tokenStatusStore.getCurrentInvalidationVersion(publicId);

        String newAccessToken = tokenProvider.issueAccessToken(publicId, member.getEmail(), invalidationVersion);
        String newRefreshToken = tokenProvider.issueRefreshToken(publicId, member.getEmail(), invalidationVersion);

        refreshTokenStore.save(publicId, newRefreshToken, tokenProvider.getRefreshTokenTtl());

        return new ReissueResult(newAccessToken, newRefreshToken);
    }

    @Transactional(readOnly = true)
    public void checkEmail(String email) {
        if (repository.existsByEmailAndDeletedAtIsNull(Member.normalizeEmail(email))) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATE);
        }
    }

    @Transactional(readOnly = true)
    public void checkNickname(String nickname) {
        if (repository.existsByNicknameAndDeletedAtIsNull(nickname)) {
            throw new BusinessException(ErrorCode.NICKNAME_DUPLICATE);
        }
    }

    private void validateMemberUniqueness(String email, String nickname) {
        if (repository.existsByEmailAndDeletedAtIsNull(email)) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATE);
        }

        if (repository.existsByNicknameAndDeletedAtIsNull(nickname)) {
            throw new BusinessException(ErrorCode.NICKNAME_DUPLICATE);
        }
    }

    private void validatePasswordMatch(String password, String passwordCheck) {
        if (!password.equals(passwordCheck)) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }
    }
}

