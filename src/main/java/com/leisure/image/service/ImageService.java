package com.leisure.image.service;

import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.global.properties.S3Properties;
import com.leisure.image.dto.request.PresignedUrlRequest;
import com.leisure.image.dto.response.PresignedUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.util.UUID;


@Service
@RequiredArgsConstructor
public class ImageService {

    private final S3Presigner s3Presigner;

    private final S3Properties s3Properties;

    public PresignedUrlResponse createPresignedUrl(PresignedUrlRequest request) {

        if (request == null || request.imagePurpose() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER);
        }

        String ext = extractExt(request.contentType());

        String objectKey = request.imagePurpose().getPrefix() + "/" + UUID.randomUUID() + "." + ext;

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                        .signatureDuration(s3Properties.presignExpiration())
                        .putObjectRequest(PutObjectRequest.builder()
                                .bucket(s3Properties.bucket())
                                .key(objectKey)
                                .contentType(request.contentType())
                                .build())
                        .build()
        );

        String imageUrl = s3Properties.publicBaseUrl() + "/" + objectKey;

        return new PresignedUrlResponse(presigned.url().toString(), imageUrl, objectKey);
    }

    private String extractExt(String contentType) {

        if (contentType == null || !contentType.contains("/")) {
            throw new BusinessException(ErrorCode.IMAGE_CONTENT_TYPE_UNSUPPORTED);
        }

        return switch (contentType.trim().toLowerCase()) {
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> throw new BusinessException(ErrorCode.IMAGE_CONTENT_TYPE_UNSUPPORTED);
        };
    }
}
