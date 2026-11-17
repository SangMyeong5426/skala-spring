package com.skala.shop.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.skala.shop.common.Response;

import lombok.extern.slf4j.Slf4j;

/**
 * 전역 예외 처리기. 서비스에서 던진 예외를 표준 Response 포맷으로 변환한다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseException.class)
    public ResponseEntity<Response<Void>> handleResponseException(ResponseException e) {
        log.warn("ResponseException: {} - {}", e.getError(), e.getMessage());
        return ResponseEntity.status(e.getError().getStatus())
                .body(Response.fail(e.getError().name(), e.getMessage()));
    }

    @ExceptionHandler(ParameterException.class)
    public ResponseEntity<Response<Void>> handleParameterException(ParameterException e) {
        log.warn("ParameterException: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Response.fail("PARAMETER_ERROR", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Response.fail("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다."));
    }
}
