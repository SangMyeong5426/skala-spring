package com.skala.shop.exception;

/**
 * 필수 입력값 검증 실패 시 발생시키는 예외.
 */
public class ParameterException extends RuntimeException {

    public ParameterException(String... fields) {
        super("필수 입력값이 누락되었거나 올바르지 않습니다: " + String.join(", ", fields));
    }
}
