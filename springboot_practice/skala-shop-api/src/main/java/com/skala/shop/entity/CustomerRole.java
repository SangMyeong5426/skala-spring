package com.skala.shop.entity;

/**
 * 고객 권한 - 관리자/고객 역할 분리(차별화 포인트).
 * ADMIN은 상품 등록/수정/삭제 등 쓰기 API를 수행할 수 있다.
 */
public enum CustomerRole {
    CUSTOMER, ADMIN
}
