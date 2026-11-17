package com.skala.shop.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import com.skala.shop.repository.CustomerRepository;
import com.skala.shop.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

/**
 * Actuator /actuator/health 커스텀 확장 - 상품/고객 데이터 건수를 함께 노출한다.
 */
@Component
@RequiredArgsConstructor
public class ShopHealthIndicator implements HealthIndicator {

    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

    @Override
    public Health health() {
        long productCount = productRepository.count();
        long customerCount = customerRepository.count();
        return Health.up()
                .withDetail("productCount", productCount)
                .withDetail("customerCount", customerCount)
                .build();
    }
}
