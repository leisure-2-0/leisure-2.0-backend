package com.leisure.member.dto.request;

import com.leisure.global.exception.ValidationMessageConstants;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileChangeRequest(

        @Size(max = 50, message = ValidationMessageConstants.NICKNAME_MAX_LENGTH)
        @Pattern(regexp = "^\\S+$", message = ValidationMessageConstants.NICKNAME_NO_SPACE)
        String nickname,

        @Size(max = 500)
        String profileImageUrl
) {}
