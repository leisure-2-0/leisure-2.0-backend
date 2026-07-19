package com.leisure.global.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessgae());
        this.errorCode = errorCode;
    }

    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
