package com.leisure.image.dto.request;

import com.leisure.global.exception.ValidationMessageConstants;
import com.leisure.image.domain.ImagePurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PresignedUrlRequest(
        @NotBlank(message = ValidationMessageConstants.IMAGE_CONTENT_TYPE_REQUIRED)
        String contentType,

        @NotNull(message = ValidationMessageConstants.IMAGE_PURPOSE_REQUIRED)
        ImagePurpose imagePurpose
) {}
