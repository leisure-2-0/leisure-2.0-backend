package com.leisure.member.dto.request;

import com.leisure.global.exception.ValidationMessageConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PasswordChangeRequest(

        @NotBlank(message = ValidationMessageConstants.PASSWORD_REQUIRED)
        @Pattern(message = ValidationMessageConstants.PASSWORD_INVALID_FORMAT,
                regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%])[a-zA-Z\\d!@#$%]{8,20}$")
        String currentPassword,

        @NotBlank(message = ValidationMessageConstants.PASSWORD_REQUIRED)
        @Pattern(message = ValidationMessageConstants.PASSWORD_INVALID_FORMAT,
                regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%])[a-zA-Z\\d!@#$%]{8,20}$")
        String newPassword,

        @NotBlank(message = ValidationMessageConstants.PASSWORD_CONFIRM_REQUIRED)
        String newPasswordConfirm) {
}
