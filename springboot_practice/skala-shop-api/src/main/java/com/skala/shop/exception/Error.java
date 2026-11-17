package com.skala.shop.exception;

import org.springframework.http.HttpStatus;

/**
 * 서비스 전역에서 사용하는 에러 코드 정의.
 */
public enum Error {

    DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 데이터를 찾을 수 없습니다."),
    DATA_DUPLICATED(HttpStatus.CONFLICT, "이미 존재하는 데이터입니다."),
    NOT_AUTHENTICATED(HttpStatus.UNAUTHORIZED, "로그인이 필요하거나 인증에 실패했습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "관리자만 접근할 수 있는 기능입니다."),
    INSUFFICIENT_FUNDS(HttpStatus.BAD_REQUEST, "보유 포인트가 부족합니다."),
    INSUFFICIENT_QUANTITY(HttpStatus.BAD_REQUEST, "취소 가능한 주문 수량을 초과했습니다."),
    OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "상품 재고가 부족합니다."),
    CONCURRENT_CONFLICT(HttpStatus.CONFLICT, "동시 요청이 많아 처리하지 못했습니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus status;
    private final String defaultMessage;

    Error(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
