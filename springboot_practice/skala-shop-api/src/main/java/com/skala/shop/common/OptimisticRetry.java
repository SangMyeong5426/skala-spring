package com.skala.shop.common;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.skala.shop.exception.Error;
import com.skala.shop.exception.ResponseException;

/**
 * 낙관적 락(@Version) 충돌 시 재시도하는 유틸리티 (동시성 제어 차별화 포인트).
 * 재고/포인트처럼 여러 요청이 동시에 같은 행을 수정할 수 있는 트랜잭션을 감싸,
 * 버전 충돌이 나면 짧은 지터(jitter) 후 새 트랜잭션으로 다시 시도한다 (재시도가 한꺼번에
 * 다시 충돌하는 걸 막기 위함). 재시도를 모두 소진하면 CONCURRENT_CONFLICT 예외로
 * 클라이언트에 재시도를 요청한다.
 */
public final class OptimisticRetry {

    private static final int DEFAULT_MAX_ATTEMPTS = 10;
    private static final int MAX_BACKOFF_MILLIS = 8;

    private OptimisticRetry() {
    }

    public static <T> T execute(Supplier<T> action) {
        return execute(DEFAULT_MAX_ATTEMPTS, action);
    }

    public static <T> T execute(int maxAttempts, Supplier<T> action) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return action.get();
            } catch (ObjectOptimisticLockingFailureException e) {
                if (attempt == maxAttempts) {
                    throw new ResponseException(Error.CONCURRENT_CONFLICT);
                }
                sleepJitter();
            }
        }
        throw new ResponseException(Error.CONCURRENT_CONFLICT);
    }

    private static void sleepJitter() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(MAX_BACKOFF_MILLIS) + 1L);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
