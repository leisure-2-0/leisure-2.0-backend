package com.leisure.global.exception;

public final class ValidationMessageConstants {

    private ValidationMessageConstants() {}

    public static final String EMAIL_REQUIRED = "이메일을 입력해 주세요.";

    public static final String EMAIL_INVALID_FORMAT = "올바른 이메일 형식이 아닙니다. (예: example@example.com)";

    public static final String PASSWORD_REQUIRED = "비밀번호를 입력해 주세요.";

    public static final String PASSWORD_INVALID_FORMAT = "비밀번호는 8~20자의 영문 대/소문자, 숫자, 특수문자를 모두 포함해야 합니다.";

    public static final String NICKNAME_REQUIRED = "닉네임을 입력해 주세요.";

    public static final String NICKNAME_MAX_LENGTH = "닉네임은 최대 50자까지 입력할 수 있습니다.";

    public static final String NICKNAME_NO_SPACE = "닉네임에 공백을 포함할 수 없습니다.";
}