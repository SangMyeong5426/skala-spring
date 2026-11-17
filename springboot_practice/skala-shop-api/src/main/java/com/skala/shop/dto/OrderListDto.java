package com.skala.shop.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 고객이 주문한 상품 목록 조회 응답.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderListDto {

    private String customerId;
    private Double customerPoint;
    private List<OrderItemDto> products;
}
