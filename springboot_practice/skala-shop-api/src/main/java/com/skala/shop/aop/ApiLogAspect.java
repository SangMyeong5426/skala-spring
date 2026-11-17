package com.skala.shop.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 컨트롤러 계층 API 호출을 가로채 요청 처리 시간과 성공/실패 여부를 로깅한다.
 * (비밀번호 등 민감 정보 노출을 막기 위해 인자값 자체는 로그에 남기지 않는다.)
 */
@Slf4j
@Aspect
@Component
public class ApiLogAspect {

    @Around("execution(* com.skala.shop.controller..*Controller.*(..))")
    public Object logApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String signature = joinPoint.getSignature().toShortString();
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.info("[API] {} 처리 완료 ({}ms)", signature, elapsed);
            return result;
        } catch (Throwable e) {
            long elapsed = System.currentTimeMillis() - start;
            log.warn("[API] {} 처리 실패 ({}ms) - {}: {}", signature, elapsed, e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }
}
