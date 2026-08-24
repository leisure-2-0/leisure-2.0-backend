package com.leisure.global.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentMember {

    /**
     * 인증이 필수인지 여부.
     *
     * true
     * - 로그인 필수 API. 인증이 없으면 AUTHENTICATION_REQUIRED 예외를 던진다.
     * - 기본값이라 속성을 생략한 기존 사용처는 전부 이 동작
     *
     * false
     * - 비로그인 허용 API(예: 공개 피드).
     * - 인증이 없으면 예외 대신 null을 주입해,
     * - "로그인했으면 회원, 아니면 익명" 분기를 컨트롤러/서비스에서 할 수 있게 한다.
     */
    boolean required() default true;
}
