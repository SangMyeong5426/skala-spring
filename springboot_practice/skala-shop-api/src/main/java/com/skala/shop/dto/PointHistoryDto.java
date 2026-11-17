package com.skala.shop.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 포인트 변동 이력 응답 (차별화 기능).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointHistoryDto {
    private String reason;
    private Double changeAmount;
    private Double balanceAfter;
    private LocalDateTime createdAt;
}
