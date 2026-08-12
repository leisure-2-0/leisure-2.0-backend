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

    EMAIL_REQUIRED(HttpStatus.UNPROCESSABLE_CONTENT, HttpStatus.UNPROCESSABLE_CONTENT.value(), "이메일은 필수 입력값입니다."),

    NICKNAME_REQUIRED(HttpStatus.UNPROCESSABLE_CONTENT, HttpStatus.UNPROCESSABLE_CONTENT.value(), "닉네임은 필수 입력값입니다."),

    EMAIL_DUPLICATE(HttpStatus.CONFLICT, HttpStatus.CONFLICT.value(), "이미 사용 중인 이메일입니다."),

    NICKNAME_DUPLICATE(HttpStatus.CONFLICT, HttpStatus.CONFLICT.value(), "이미 사용 중인 닉네임입니다."),

    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value(), "비밀번호가 일치하지 않습니다."),

    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, HttpStatus.UNAUTHORIZED.value(), "이메일 또는 비밀번호가 일치하지 않습니다."),

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.value(), "존재하지 않는 회원입니다."),

    MEMBER_UPDATE_ACCESS_FORBIDDEN(HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN.value(), "회원 수정 권한이 없습니다."),

    MEMBER_DELETE_ACCESS_FORBIDDEN(HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN.value(), "회원 삭제 권한이 없습니다."),

    MEMBER_UPDATE_EMPTY(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value(), "수정할 정보가 없습니다."),

    ;


    private final HttpStatus status;

    private final int code;

    private final String messgae;

    ErrorCode(HttpStatus status, int code, String messgae) {
        this.status = status;
        this.code = code;
        this.messgae = messgae;
    }
}
