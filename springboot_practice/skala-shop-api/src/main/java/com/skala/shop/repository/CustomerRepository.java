package com.skala.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skala.shop.entity.Customer;

public interface CustomerRepository extends JpaRepository<Customer, String> {
    // 사용자 정의 메서드 필요 없음 (PK가 곧 customerId)
}
