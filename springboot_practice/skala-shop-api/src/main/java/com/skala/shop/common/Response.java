package com.skala.shop.common;

import lombok.Getter;

/**
 * 모든 API 응답을 감싸는 표준 응답 포맷.
 */
@Getter
public class Response<T> {

    private final boolean success;
    private final String code;
    private final String message;
    private final T data;

    private Response(boolean success, String code, String message, T data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Response<T> ok(T data) {
        return new Response<>(true, "OK", "요청이 정상 처리되었습니다.", data);
    }

    public static <T> Response<T> ok(String message, T data) {
        return new Response<>(true, "OK", message, data);
    }

    public static <T> Response<T> fail(String code, String message) {
        return new Response<>(false, code, message, null);
    }
}
