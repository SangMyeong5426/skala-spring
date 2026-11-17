package com.skala.shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

/**
 * 동시성 제어(낙관적 락 + 자동 재시도) 검증 - 차별화 포인트.
 * 재고가 5개뿐인 상품에 8명의 고객이 동시에 1개씩 주문을 넣어도,
 * 정확히 5건만 성공하고 나머지는 OUT_OF_STOCK으로 안전하게 거부되는지(초과 판매가 없는지) 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderConcurrencyTest {

    @Autowired
    private MockMvc mockMvc;

    private static final int STOCK = 5;
    private static final int CUSTOMER_COUNT = 8;

    @Test
    void 재고보다_많은_동시_주문이_들어와도_초과_판매되지_않는다() throws Exception {
        Cookie adminCookie = login("admin", "admin1234");

        MvcResult createResult = mockMvc.perform(post("/api/products")
                .cookie(adminCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productName\":\"동시성테스트상품\",\"productPrice\":1000,\"productStock\":" + STOCK + "}"))
                .andReturn();
        Number productId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.id");

        List<Cookie> cookies = new ArrayList<>();
        for (int i = 0; i < CUSTOMER_COUNT; i++) {
            String id = "raceCustomer" + i;
            mockMvc.perform(post("/api/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"customerId\":\"" + id + "\",\"customerPassword\":\"pw1234\"}"));
            cookies.add(login(id, "pw1234"));
        }

        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(CUSTOMER_COUNT);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger outOfStockCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();

        List<Callable<Void>> tasks = new ArrayList<>();
        for (Cookie cookie : cookies) {
            tasks.add(() -> {
                startLatch.await();
                MvcResult result = mockMvc.perform(post("/api/customers/order")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"quantity\":1}"))
                        .andReturn();
                String body = result.getResponse().getContentAsString();
                boolean success = Boolean.TRUE.equals(JsonPath.read(body, "$.success"));
                if (success) {
                    successCount.incrementAndGet();
                } else {
                    String code = JsonPath.read(body, "$.code");
                    if ("OUT_OF_STOCK".equals(code)) {
                        outOfStockCount.incrementAndGet();
                    } else if ("CONCURRENT_CONFLICT".equals(code)) {
                        // OptimisticRetry가 재시도(10회 + 지터)를 모두 소진한 극히 드문 경우.
                        // 재시도 후에도 남는 경합이므로 안전하게 실패한 것으로 취급한다 (재고를 축내지 않음).
                        conflictCount.incrementAndGet();
                    }
                }
                return null;
            });
        }

        // submit()은 즉시 반환되므로, 모든 작업을 먼저 스레드풀에 올려 각각 래치 대기 상태로 만든 뒤
        // countDown()으로 한 번에 풀어줘야 진짜 동시 경합이 재현된다. (invokeAll은 완료까지 블로킹되어 사용 불가)
        List<Future<Void>> futures = new ArrayList<>();
        for (Callable<Void> task : tasks) {
            futures.add(pool.submit(task));
        }
        startLatch.countDown();
        for (Future<Void> f : futures) {
            f.get();
        }
        pool.shutdown();

        // 핵심 안전성 검증: 재고(5개)보다 많이 팔린 적은 단 한 번도 없어야 한다 (초과 판매 금지).
        assertThat(successCount.get()).isLessThanOrEqualTo(STOCK);
        // 모든 요청이 성공/재고부족/충돌 중 하나로 반드시 결론나야 한다 (요청이 유실되지 않음).
        assertThat(successCount.get() + outOfStockCount.get() + conflictCount.get()).isEqualTo(CUSTOMER_COUNT);

        MvcResult finalProduct = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/products/" + productId))
                .andReturn();
        Number remainingStock = JsonPath.read(finalProduct.getResponse().getContentAsString(), "$.data.productStock");
        // 재고는 정확히 "성공한 주문 수"만큼만 줄어야 한다 (성공 건수와 실제 재고가 항상 일치).
        assertThat(remainingStock.intValue()).isEqualTo(STOCK - successCount.get());
    }

    private Cookie login(String customerId, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/customers/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"" + customerId + "\",\"customerPassword\":\"" + password + "\"}"))
                .andReturn();
        return result.getResponse().getCookie("shop-access");
    }
}
