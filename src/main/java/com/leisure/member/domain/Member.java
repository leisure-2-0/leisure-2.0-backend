package com.leisure.member.domain;

import com.leisure.global.entity.BaseSoftDeleteEntity;
import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Locale;
import java.util.UUID;


@Entity
@Table(name = "members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Member extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "public_id", unique = true, nullable = false, updatable = false, length = 36)
    private String publicId;

    @Column(name = "email", unique = true, nullable = false, updatable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "nickname", unique = true, nullable = false, length = 50)
    private String nickname;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    private Member(String email, String password, String nickname, String profileImageUrl) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
    }

    public static Member create(String email, String password, String nickname, String profileImageUrl) {
        return new Member(email, password, nickname, normalizeProfileImageUrl(profileImageUrl));
    }

    @PrePersist
    private void assignPublicId() {
        if (publicId == null) {
            publicId = String.valueOf(UUID.randomUUID());
        }
    }

    public void changeNickname(String nickname) {
        if (nickname.isBlank()) {
            throw new BusinessException(ErrorCode.NICKNAME_REQUIRED);
        }
        this.nickname = nickname.trim();
    }

    public void changeProfileImageUrl(String profileImageUrl) {
        if (profileImageUrl.isBlank()) {
            this.profileImageUrl = null;
            return;
        }
        this.profileImageUrl = profileImageUrl.trim();
    }

    public void changePassword(String password) {
        this.password = password;
    }

    public boolean matchesPassword(String rawPassword, PasswordEncoder encoder) {
        return encoder.matches(rawPassword, this.password);
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeProfileImageUrl(String profileImageUrl) {
        if (profileImageUrl == null || profileImageUrl.isBlank()) {
            return null;
        }

        return profileImageUrl.trim();
    }
}