package com.skala.shop.controller;

import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skala.shop.common.OptimisticRetry;
import com.skala.shop.common.PagedList;
import com.skala.shop.common.Response;
import com.skala.shop.common.SessionHandler;
import com.skala.shop.dto.CustomerSession;
import com.skala.shop.dto.LoginResponseDto;
import com.skala.shop.dto.OrderItemDto;
import com.skala.shop.dto.OrderListDto;
import com.skala.shop.dto.OrderRequest;
import com.skala.shop.dto.PointHistoryDto;
import com.skala.shop.entity.Customer;
import com.skala.shop.service.AuthService;
import com.skala.shop.service.CustomerService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final AuthService authService;

    // 전체 고객 목록 조회
    // 명세서 내 API 목록 표(GET /api/customers)와 Controller 상세 절(@GetMapping("/list"))이
    // 서로 다른 경로를 제시하고 있어, 두 경로를 모두 지원하도록 처리했다.
    @GetMapping({"", "/list"})
    public Response<PagedList<Customer>> getAllCustomers(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int count) {
        return customerService.getAllCustomers(offset, count);
    }

    // 단일 고객 상세 + 주문 상품 목록 조회
    @GetMapping("/{customerId}")
    public Response<OrderListDto> getCustomerById(@PathVariable String customerId) {
        return customerService.getCustomerById(customerId);
    }

    // 고객의 주문 상품 목록만 조회 (API 명세의 별도 엔드포인트 - getCustomerById와 동일 데이터의 서브셋)
    @GetMapping("/{customerId}/products")
    public Response<List<OrderItemDto>> getCustomerProducts(@PathVariable String customerId) {
        return Response.ok(customerService.getCustomerById(customerId).getData().getProducts());
    }

    // 고객의 포인트 변동 이력 조회 (차별화 기능)
    @GetMapping("/{customerId}/points")
    public Response<List<PointHistoryDto>> getPointHistory(@PathVariable String customerId) {
        return customerService.getPointHistory(customerId);
    }

    // 고객 등록 (회원가입)
    @PostMapping
    public Response<Customer> createCustomer(@RequestBody Customer customer) {
        return customerService.createCustomer(customer);
    }

    // 고객 로그인 - 성공 시 세션 토큰을 Cookie로 발급 (JWT 미사용)
    @PostMapping("/login")
    public Response<LoginResponseDto> loginCustomer(@RequestBody CustomerSession session, HttpServletResponse response) {
        Response<LoginResponseDto> result = customerService.loginCustomer(session);

        Cookie cookie = new Cookie(SessionHandler.COOKIE_NAME, result.getData().getToken());
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(60 * 60); // 1시간
        response.addCookie(cookie);

        return result;
    }

    // 로그아웃
    @PostMapping("/logout")
    public Response<Void> logout(@CookieValue(value = SessionHandler.COOKIE_NAME, required = false) String token,
                                  HttpServletResponse response) {
        Cookie cookie = new Cookie(SessionHandler.COOKIE_NAME, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return customerService.logout(token);
    }

    // 고객 정보 수정 (포인트)
    @PutMapping
    public Response<Customer> updateCustomer(@RequestBody Customer customer) {
        return customerService.updateCustomer(customer);
    }

    // 고객 삭제
    @DeleteMapping("/{customerId}")
    public Response<Void> deleteCustomer(@PathVariable String customerId) {
        return customerService.deleteCustomer(customerId);
    }

    // 상품 주문 - 로그인 필수 (Cookie의 세션 토큰에서 고객을 식별)
    // 재고/포인트에 걸린 낙관적 락(@Version) 충돌 시 OptimisticRetry가 자동으로 재시도한다 (동시성 제어 - 차별화 포인트).
    @PostMapping("/order")
    public Response<OrderListDto> placeOrder(@CookieValue(value = SessionHandler.COOKIE_NAME, required = false) String token,
                                              @RequestBody OrderRequest order) {
        String customerId = authService.requireLogin(token);
        return OptimisticRetry.execute(() -> customerService.placeOrder(customerId, order));
    }

    // 주문 취소 - 로그인 필수 (동시성 제어 재시도 동일 적용)
    @PostMapping("/cancel")
    public Response<OrderListDto> cancelOrder(@CookieValue(value = SessionHandler.COOKIE_NAME, required = false) String token,
                                               @RequestBody OrderRequest order) {
        String customerId = authService.requireLogin(token);
        return OptimisticRetry.execute(() -> customerService.cancelOrder(customerId, order));
    }
}
