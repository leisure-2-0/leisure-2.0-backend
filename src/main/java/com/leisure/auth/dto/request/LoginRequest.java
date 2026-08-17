package com.leisure.auth.dto.request;

import com.leisure.global.exception.ValidationMessageConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(

        @NotBlank(message = ValidationMessageConstants.EMAIL_REQUIRED)
        @Email(message = ValidationMessageConstants.EMAIL_INVALID_FORMAT)
        String email,

        @NotBlank(message = ValidationMessageConstants.PASSWORD_REQUIRED)
        @Pattern(message = ValidationMessageConstants.PASSWORD_INVALID_FORMAT,
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\\W_]).{8,20}$")
        String password
) {
}
