package com.skala.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 상품 엔터티. ID는 순차 자동 증가.
 * stock(재고)은 명세서 기본 스펙에는 없는 확장 필드 - 재고 관리 차별화 포인트.
 */
@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String productName;

    @Column(nullable = false)
    private Double productPrice;

    @Column(nullable = false)
    private Integer productStock = 0;

    // 낙관적 락(차별화 포인트) - 동시에 여러 주문이 재고를 차감할 때 초과 판매를 방지한다.
    @Version
    private Long version;

    public Product(String productName, Double productPrice) {
        this.productName = productName;
        this.productPrice = productPrice;
        this.productStock = 0;
    }

    public Product(String productName, Double productPrice, Integer productStock) {
        this.productName = productName;
        this.productPrice = productPrice;
        this.productStock = productStock;
    }
}
