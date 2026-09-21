package com.leisure.image.controller;

import com.leisure.global.auth.CurrentMember;
import com.leisure.global.response.ApiResponse;
import com.leisure.image.dto.request.PresignedUrlRequest;
import com.leisure.image.dto.response.PresignedUrlResponse;
import com.leisure.image.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "이미지", description = "이미지 업로드(presigned URL)")
@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @Operation(
            summary = "이미지 업로드용 presigned URL 발급",
            description = """
                    S3 presigned PUT URL을 발급한다. 인증 필요.
                    - 응답 `presignedUrl`로 파일을 직접 PUT (요청한 `contentType`과 동일한 Content-Type 헤더 필수)
                    - 업로드 후 `imageUrl`(CloudFront)을 프로필/게시글 본문 등에 저장
                    - URL은 짧은 시간(기본 5분) 후 만료
                    """
    )
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> createPresignedUrl(
            @CurrentMember String publicId,
            @Valid @RequestBody PresignedUrlRequest request) {

        PresignedUrlResponse response = imageService.createPresignedUrl(request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("presigned URL 발급 성공", response));
    }
}
