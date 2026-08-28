package com.leisure.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * ErrorCode 네이밍 패턴
 *
 * 패턴 A: {도메인}_{상태}
 * 무엇이 어떤 상태인가
 *
 * 패턴 B: {상황}_{대상} (중복 검증류)
 * 상황(중복/유효하지 않음/만료)이 앞, 대상이 뒤
 *
 * 패턴 C: {도메인}_{행위}_{상태} (권한 금지류)
 * 어느 도메인의 어떤 행위가 어떻게 됐는가
 *
 * 패턴 D: 공통/시스템류 (도메인 없음)
 * 특정 도메인에 속하지 않는 공통 에러, HTTP 표준 코드명을 따라가는 경우가 많음
 */
@Getter
public enum ErrorCode {

    // ===== 회원 / 도메인 =====
    EMAIL_REQUIRED(HttpStatus.UNPROCESSABLE_CONTENT, "이메일은 필수 입력값입니다."),

    NICKNAME_REQUIRED(HttpStatus.UNPROCESSABLE_CONTENT, "닉네임은 필수 입력값입니다."),

    EMAIL_DUPLICATE(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),

    NICKNAME_DUPLICATE(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),

    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),

    MEMBER_UPDATE_ACCESS_FORBIDDEN(HttpStatus.FORBIDDEN, "회원 수정 권한이 없습니다."),

    MEMBER_DELETE_ACCESS_FORBIDDEN(HttpStatus.FORBIDDEN, "회원 삭제 권한이 없습니다."),

    MEMBER_UPDATE_EMPTY(HttpStatus.BAD_REQUEST, "수정할 정보가 없습니다."),

    // ===== 인증 / 토큰 =====
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다."),

    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),

    TOKEN_UNSUPPORTED(HttpStatus.BAD_REQUEST, "지원하지 않는 형식의 토큰입니다."),

    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),

    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "refresh token을 찾을 수 없습니다. 다시 로그인 해주세요."),

    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "만료된 refresh token입니다. 다시 로그인 해주세요."),

    REFRESH_TOKEN_REUSE_DETECTED(HttpStatus.UNAUTHORIZED, "이미 사용된 토큰입니다. 다시 로그인 해주세요."),

    TOKEN_BLACKLISTED(HttpStatus.UNAUTHORIZED, "로그아웃된 토큰입니다."),

    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),

    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // ===== 게시글 / 도메인 =====
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."),

    POST_FORBIDDEN(HttpStatus.FORBIDDEN, "해당 게시글에 대한 권한이 없습니다."),

    POST_NOT_SUBMITTABLE(HttpStatus.BAD_REQUEST, "현재 상태에서는 게시글을 게시할 수 없습니다."),

    POST_TITLE_REQUIRED(HttpStatus.BAD_REQUEST, "제목을 입력해야 게시할 수 있습니다."),

    POST_NOT_PENDING(HttpStatus.CONFLICT, "승인 대기 상태의 게시글만 처리할 수 있습니다."),

    POST_NOT_EDITABLE(HttpStatus.CONFLICT, "현재 상태에서는 게시글을 수정할 수 없습니다."),

    POST_TAG_INVALID(HttpStatus.BAD_REQUEST, "유효하지 않은 태그입니다."),

    // ===== 공통 / 시스템 표준 예외 매핑 =====
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),

    INVALID_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "잘못된 요청 파라미터입니다."),

    MISSING_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "필수 요청 파라미터가 누락되었습니다."),

    INVALID_CURSOR(HttpStatus.BAD_REQUEST, "유효하지 않은 커서입니다."),

    PAGE_INVALID(HttpStatus.BAD_REQUEST, "페이지 번호는 0 이상이어야 합니다."),

    PAGE_SIZE_INVALID(HttpStatus.BAD_REQUEST, "페이지 크기는 1 이상 30 이하이어야 합니다."),

    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 요청 형식입니다."),

    VALIDATION_FAILED(HttpStatus.UNPROCESSABLE_CONTENT, "입력값 검증에 실패했습니다."),

    PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "파일 크기가 허용치를 초과했습니다."),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부에 오류가 발생했습니다."),

    // ===== 쪼아요~ 좋아요~ =====
    POST_LIKE_ALREADY_LIKED(HttpStatus.CONFLICT, "이미 좋아요를 누른 게시글입니다."),

    POST_LIKE_NOT_LIKED_YET(HttpStatus.NOT_FOUND, "좋아요를 누르지 않은 게시글입니다."),

    // ===== 북마크 =====
    POST_BOOKMARK_ALREADY_BOOKMARKED(HttpStatus.CONFLICT, "이미 북마크한 게시글입니다."),

    POST_BOOKMARK_NOT_BOOKMARKED_YET(HttpStatus.NOT_FOUND, "북마크하지 않은 게시글입니다."),

    ;

    private final HttpStatus status;

    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
