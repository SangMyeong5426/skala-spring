package com.skala.shop.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.skala.shop.entity.Customer;
import com.skala.shop.entity.CustomerRole;
import com.skala.shop.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 관리자/고객 역할 분리(차별화 포인트) - 애플리케이션 기동 시 기본 관리자 계정이 없으면 생성한다.
 * 회원가입 API로는 항상 CUSTOMER만 만들어지므로, 관리자 계정은 이 초기화 과정으로만 확보된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner {

    private static final String ADMIN_ID = "admin";
    private static final String ADMIN_PASSWORD = "admin1234";

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (customerRepository.existsById(ADMIN_ID)) {
            return;
        }
        Customer admin = new Customer(ADMIN_ID, 0.0, CustomerRole.ADMIN);
        admin.setCustomerPassword(passwordEncoder.encode(ADMIN_PASSWORD));
        customerRepository.save(admin);
        log.info("기본 관리자 계정을 생성했습니다. (id: {}, pw: {})", ADMIN_ID, ADMIN_PASSWORD);
    }
}
