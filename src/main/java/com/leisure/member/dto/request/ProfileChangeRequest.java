package com.leisure.member.dto.request;

import com.leisure.global.exception.ValidationMessageConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileChangeRequest(

        @Schema(description = "변경할 닉네임. null이면 기존 닉네임 유지. 공백은 불가.")
        @Size(max = 50, message = ValidationMessageConstants.NICKNAME_MAX_LENGTH)
        @Pattern(regexp = "^\\S+$", message = ValidationMessageConstants.NICKNAME_NO_SPACE)
        String nickname,

        @Schema(description = "프로필 이미지 URL. null이면 기존 이미지 유지, 빈 문자열 또는 공백이면 이미지 제거(null로 저장).")
        @Size(max = 500)
        String profileImageUrl
) {}
