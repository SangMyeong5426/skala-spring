package com.skala.shop.service;

import org.springframework.stereotype.Service;

import com.skala.shop.common.SessionHandler;
import com.skala.shop.entity.Customer;
import com.skala.shop.entity.CustomerRole;
import com.skala.shop.exception.Error;
import com.skala.shop.exception.ResponseException;
import com.skala.shop.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

/**
 * 컨트롤러 계층에서 공통으로 사용하는 인증/인가 확인 로직.
 * ProductController(관리자 전용 쓰기 API)와 CustomerController(로그인 필수 API)에서 함께 사용한다.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SessionHandler sessionHandler;
    private final CustomerRepository customerRepository;

    // 쿠키의 세션 토큰으로 로그인된 고객 ID를 확인. 없으면 인증 예외.
    public String requireLogin(String token) {
        String customerId = sessionHandler.resolveCustomerId(token);
        if (customerId == null) {
            throw new ResponseException(Error.NOT_AUTHENTICATED, "로그인이 필요합니다.");
        }
        return customerId;
    }

    // 로그인 + 관리자 권한(ADMIN)까지 확인 (관리자/고객 역할 분리 - 차별화 포인트).
    public Customer requireAdmin(String token) {
        String customerId = requireLogin(token);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND, "고객을 찾을 수 없습니다."));
        if (customer.getRole() != CustomerRole.ADMIN) {
            throw new ResponseException(Error.FORBIDDEN);
        }
        return customer;
    }
}
