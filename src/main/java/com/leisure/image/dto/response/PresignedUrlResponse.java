package com.leisure.image.dto.response;

public record PresignedUrlResponse(
        String presignedUrl,   // 업로드용 presigned PUT URL

        String imageUrl,       // 업로드 후 조회 URL (CloudFront)

        String objectKey       // S3 오브젝트 키
) {}
