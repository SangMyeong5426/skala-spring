package com.skala.shop.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.stereotype.Component;

/**
 * Actuator 커스텀 메트릭 (차별화 포인트) - 주문/취소 건수와 누적 매출을
 * Micrometer Counter로 등록해 /actuator/metrics 에서 확인할 수 있게 한다.
 */
@Component
public class ShopMetrics {

    private final Counter orderCounter;
    private final Counter cancelCounter;
    private final Counter revenueCounter;

    public ShopMetrics(MeterRegistry registry) {
        this.orderCounter = Counter.builder("shop.orders.count")
                .description("성공적으로 처리된 주문 건수")
                .register(registry);
        this.cancelCounter = Counter.builder("shop.orders.cancelled.count")
                .description("취소된 주문 건수")
                .register(registry);
        this.revenueCounter = Counter.builder("shop.revenue.total")
                .description("누적 주문 매출액 (취소분 차감 없이 주문 총액 누계)")
                .register(registry);
    }

    public void recordOrder(double amount) {
        orderCounter.increment();
        revenueCounter.increment(amount);
    }

    public void recordCancel() {
        cancelCounter.increment();
    }
}
