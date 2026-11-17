package com.skala.shop.exception;

import lombok.Getter;

/**
 * 비즈니스 규칙 위반 시 발생시키는 예외 (없는 데이터 조회, 포인트/재고 부족 등).
 */
@Getter
public class ResponseException extends RuntimeException {

    private final Error error;

    public ResponseException(Error error) {
        super(error.getDefaultMessage());
        this.error = error;
    }

    public ResponseException(Error error, String message) {
        super(message);
        this.error = error;
    }
}
