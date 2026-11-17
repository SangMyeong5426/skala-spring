package com.skala.shop.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 상품 주문 / 주문 취소 요청.
 */
@Getter
@Setter
@NoArgsConstructor
public class OrderRequest {
    private Long productId;
    private Integer quantity;
}
