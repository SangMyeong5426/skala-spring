package com.skala.shop.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 고객 포인트 변동 이력 - 명세서 기본 스펙에는 없는 확장 엔터티(차별화 포인트).
 * 가입/주문/취소 시점의 포인트 증감과 잔액 스냅샷을 남겨 감사(audit) 추적을 가능하게 한다.
 */
@Entity
@Table(name = "point_history")
@Getter
@Setter
@NoArgsConstructor
public class PointHistory {

    public enum Reason {
        SIGNUP, ORDER, CANCEL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    private Reason reason;

    private Double changeAmount;

    private Double balanceAfter;

    private LocalDateTime createdAt;

    public PointHistory(Customer customer, Reason reason, Double changeAmount, Double balanceAfter) {
        this.customer = customer;
        this.reason = reason;
        this.changeAmount = changeAmount;
        this.balanceAfter = balanceAfter;
        this.createdAt = LocalDateTime.now();
    }
}
