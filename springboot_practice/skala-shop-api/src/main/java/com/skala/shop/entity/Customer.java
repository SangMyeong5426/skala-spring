package com.skala.shop.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 고객 엔터티. ID는 문자열 customerId (자연키).
 */
@Entity
@Table(name = "customer")
@Getter
@Setter
@NoArgsConstructor
public class Customer {

    @Id
    private String customerId;

    // 응답 JSON에는 절대 내려가지 않도록 write-only 처리 (요청 바디에서 읽는 것은 그대로 허용).
    // 예전에는 서비스 코드에서 customer.setCustomerPassword(null) 로 응답 직전에 지웠는데,
    // 그 변경이 관리 중인 엔터티에 그대로 반영되어 커밋 시점에 실제 UPDATE로 플러시되는 버그가 있었다
    // (버전 컬럼이 추가되며 매 수정마다 실제 UPDATE가 나가게 되어 NOT NULL 제약 위반으로 드러남).
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String customerPassword;

    @Column(nullable = false)
    private Double customerPoint;

    // 관리자/고객 역할 분리(차별화 포인트) - 회원가입으로는 항상 CUSTOMER만 생성된다.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomerRole role = CustomerRole.CUSTOMER;

    // 낙관적 락(차별화 포인트) - 동시 주문/취소로 포인트가 동시에 갱신될 때의 정합성을 보장한다.
    @Version
    private Long version;

    public Customer(String customerId, Double customerPoint) {
        this.customerId = customerId;
        this.customerPoint = customerPoint;
        this.role = CustomerRole.CUSTOMER;
    }

    public Customer(String customerId, Double customerPoint, CustomerRole role) {
        this.customerId = customerId;
        this.customerPoint = customerPoint;
        this.role = role;
    }
}
