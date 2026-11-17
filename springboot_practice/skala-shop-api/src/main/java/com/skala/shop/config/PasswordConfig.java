package com.skala.shop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * spring-security-crypto만 단독으로 사용해 비밀번호를 BCrypt로 해싱한다.
 * (spring-boot-starter-security를 도입하지 않으므로 시큐리티 필터체인/기본 로그인 폼 등은 전혀 개입하지 않는다.)
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
