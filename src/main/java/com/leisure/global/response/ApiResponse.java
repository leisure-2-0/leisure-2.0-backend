package com.leisure.global.response;

public record ApiResponse<T>(boolean success, String name, String message, T data) {

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, null, message, data);
    }

    public static ApiResponse<Void> fail(String name, String message) {
        return new ApiResponse<>(false, name, message, null);
    }

    public static <T> ApiResponse<T> fail(String name, String message, T data) {
        return new ApiResponse<>(false, name, message, data);
    }
}