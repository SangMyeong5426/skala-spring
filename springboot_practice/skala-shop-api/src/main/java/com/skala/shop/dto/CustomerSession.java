package com.skala.shop.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 고객 로그인 요청 바디.
 */
@Getter
@Setter
@NoArgsConstructor
public class CustomerSession {
    private String customerId;
    private String customerPassword;
}
