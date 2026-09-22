package com.leisure.image.service;

import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.global.properties.S3Properties;
import com.leisure.image.domain.ImagePurpose;
import com.leisure.image.dto.request.PresignedUrlRequest;
import com.leisure.image.dto.response.PresignedUrlResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    private static final String BASE_URL = "https://d4sz6l1f6v7xn.cloudfront.net";

    @Mock
    private S3Presigner s3Presigner;

    private ImageService imageService;

    @BeforeEach
    void setUp() {
        S3Properties properties = new S3Properties("leisure-images", "ap-northeast-2", BASE_URL, Duration.ofMinutes(5));
        imageService = new ImageService(s3Presigner, properties);
    }

    @Nested
    @DisplayName("presigned URL 발급")
    class CreatePresignedUrl {

        @Test
        @DisplayName("정상: presignedUrl/imageUrl/objectKey를 반환한다")
        void success() throws Exception {
            PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
            when(presigned.url()).thenReturn(URI.create("https://leisure-images.s3/put").toURL());
            when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presigned);

            PresignedUrlResponse response = imageService.createPresignedUrl(
                    new PresignedUrlRequest("image/jpeg", ImagePurpose.THUMBNAIL));

            assertThat(response.presignedUrl()).isEqualTo("https://leisure-images.s3/put");
            assertThat(response.objectKey()).startsWith("thumbnails/").endsWith(".jpg");
            assertThat(response.imageUrl()).startsWith(BASE_URL + "/thumbnails/").endsWith(".jpg");
        }

        @Test
        @DisplayName("purpose에 따라 key 접두가 달라진다 (PROFILE → profiles/)")
        void profilePrefix() throws Exception {
            PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
            when(presigned.url()).thenReturn(URI.create("https://leisure-images.s3/put").toURL());
            when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presigned);

            PresignedUrlResponse response = imageService.createPresignedUrl(
                    new PresignedUrlRequest("image/png", ImagePurpose.PROFILE));

            assertThat(response.objectKey()).startsWith("profiles/").endsWith(".png");
        }

        @Test
        @DisplayName("지원하지 않는 content-type이면 IMAGE_CONTENT_TYPE_UNSUPPORTED")
        void unsupportedContentType() {
            assertThatThrownBy(() -> imageService.createPresignedUrl(
                    new PresignedUrlRequest("application/pdf", ImagePurpose.THUMBNAIL)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.IMAGE_CONTENT_TYPE_UNSUPPORTED);
        }

        @Test
        @DisplayName("content-type 형식이 아니면 IMAGE_CONTENT_TYPE_UNSUPPORTED")
        void invalidContentType() {
            assertThatThrownBy(() -> imageService.createPresignedUrl(
                    new PresignedUrlRequest("jpeg", ImagePurpose.THUMBNAIL)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.IMAGE_CONTENT_TYPE_UNSUPPORTED);
        }

        @Test
        @DisplayName("purpose가 null이면 INVALID_REQUEST_PARAMETER")
        void nullPurpose() {
            assertThatThrownBy(() -> imageService.createPresignedUrl(
                    new PresignedUrlRequest("image/jpeg", null)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_REQUEST_PARAMETER);
        }
    }
}
