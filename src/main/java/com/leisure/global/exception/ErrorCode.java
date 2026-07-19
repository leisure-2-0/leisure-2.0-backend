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
