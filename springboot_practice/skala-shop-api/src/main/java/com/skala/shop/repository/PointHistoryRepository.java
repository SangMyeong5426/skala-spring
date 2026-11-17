package com.skala.shop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skala.shop.entity.Customer;
import com.skala.shop.entity.PointHistory;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    List<PointHistory> findByCustomer_CustomerIdOrderByCreatedAtDesc(String customerId);

    // 고객 삭제 시 FK 제약 위반을 막기 위해 연관 포인트 이력을 함께 정리
    void deleteByCustomer(Customer customer);
}
