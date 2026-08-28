package com.leisure.global.exception;

import com.leisure.global.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;

/**
 * @Valid, Standard Exception, Business Exception 그 외 예상 못 한 예외를 잡아
 * 각 핸들러 메서드로 분기시키는 전역 예외 처리 클래스
 * 예외를 처리한 후 공통 API 응답 형식으로 클라이언트에게 반환
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * 비즈니스 로직 예외가 아닌 표준 예외는
     * private static final 상수로 선언 혹은
     * 비즈니스 예외와 다른 Enum으로 분리해서 관리할 것
     */
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ResponseEntity<ApiResponse<Void>> toResponse(ErrorCode errorCode) {

        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode.name(), errorCode.getMessage()));
    }

    /**
     * BusinessException
     * <p>
     * 서비스 계층에서 도메인 규칙 위반 시 던지는 직접 정의한 커스텀 예외
     * 이메일 중복, 사용자 없음, 권한 부족, 상태 충돌 등 비즈니스 규칙이 깨진 경우
     * ErrorCode 안에 들어있는 속성들을 가져와 프론트에게 반환
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        return toResponse(e.getErrorCode());
    }

    /**
     * HttpMessageNotReadableException
     * <p>
     * Spring이 요청 Body를 객체로 변환하지 못했을 때 발생
     * JSON 문법 오류, 닫는 괄호 누락, 필드 타입 불일치로 Jackson 파싱이 되지 않은 경우
     * 클라이언트 측 형식 오류이므로 400을 반환
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadableException(HttpMessageNotReadableException e) {
        return toResponse(ErrorCode.INVALID_REQUEST_BODY);
    }

    /**
     * MethodArgumentTypeMismatchException
     * <p>
     * Spring이 요청 파라미터를 컨트롤러 메서드 인자 타입으로 변환하지 못했을 때 발생
     * 쿼리스트링의 enum 값 오류, 숫자 타입 경로변수에 문자열 입력 같은 경우
     * 클라이언트 측 파라미터 형식 오류이므로 400을 반환
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        return toResponse(ErrorCode.INVALID_REQUEST_PARAMETER);
    }

    /**
     * MissingServletRequestParameterException
     *
     * @RequestParam 필수값이 채워지지 않은 채로 컨트롤러에 도달했을 때 발생
     * page, size / limit, offset / cursor, after, before, nextToken 같은 쿼리스트링 필수 파라미터가 빠진 GET 요청
     * 인자를 채울 수 없는 클라이언트 측 누락이므로 400을 반환
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        return toResponse(ErrorCode.MISSING_REQUEST_PARAMETER);
    }

    /**
     * MaxUploadSizeExceededException
     *
     * application.yml의 설정한 spring.servlet.multipart 설정값을 초과한 파일이 업로드되었을 때 발생
     * 단일 파일이 2MB를 넘는 경우, multipart 요청 전체 크기가 max-request-size를 넘는 경우 발생
     * 클라이언트 보낸 페이로드 서버 허용치 초과한 것이므 413을 반환
     *
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        return toResponse(ErrorCode.PAYLOAD_TOO_LARGE);
    }

    /**
     * HttpMediaTypeNotSupportedException
     * <p>
     * 서버가 지원하지 않는 Content-Type으로 요청이 들어왔을 때 발생하는 예외를 처리
     * application/json만 받는 API에 text/plain 요청, XML 요청
     * 서버가 지원하지 않는 요청 형식이므로 415를 반환
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        return toResponse(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    /**
     * MethodArgumentNotValidException
     *
     * @Valid/@Validated로 검증 실패 시 발생
     * Body는 객체로 변환 성공, 그 다음 단계인 검증에서 실패
     * 예: @NotBlank, @Email, @Size 같은 애노테이션 위반
     * Spring 기준 표준은 400 (자동 처리 시 기본 반환값)
     * HTTP 표준 의미상으로는 422
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {

        List<String> messages = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();

        ErrorCode errorCode = ErrorCode.VALIDATION_FAILED;

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode.name(), "입력값 검증에 실패했습니다.", messages));
    }


    /**
     * Exception
     * <p>
     * 위에서 처리하지 못한 모든 예외를 처리하는 풀백 핸들러
     * 위 핸들러들이 잡지 못한 모든 예외를 받는 최종 안전망
     * 예상하지 못한 서버 내부 오류
     * 서버 측 책임이므로 500을 반환
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        return toResponse(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
