package com.leisure.member.dto.response;

public record SignUpResponse(String publicId) {

    public SignUpResponse {
        if (publicId == null || publicId.isBlank()) {
            throw new IllegalArgumentException("publicId는 비어 있을 수 없습니다.");
        }
    }
}
