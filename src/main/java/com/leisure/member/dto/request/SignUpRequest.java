package com.leisure.member.dto.request;

import com.leisure.global.exception.ValidationMessageConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignUpRequest(

        @NotBlank(message = ValidationMessageConstants.EMAIL_REQUIRED)
        @Email(message = ValidationMessageConstants.EMAIL_INVALID_FORMAT)
        String email,

        @NotBlank(message = ValidationMessageConstants.PASSWORD_REQUIRED)
        @Pattern(message = ValidationMessageConstants.PASSWORD_INVALID_FORMAT,
                regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[\\W_]).{8,20}$")
        String password,

        @NotBlank
        String passwordCheck,

        @NotBlank(message = ValidationMessageConstants.NICKNAME_REQUIRED)
        @Size(max = 50, message = ValidationMessageConstants.NICKNAME_MAX_LENGTH)
        @Pattern(message = ValidationMessageConstants.NICKNAME_NO_SPACE, regexp = "^\\S+$")
        String nickname,

        String profileImageUrl
) {
}
