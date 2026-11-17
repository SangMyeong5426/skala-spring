package com.skala.shop.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skala.shop.entity.Customer;
import com.skala.shop.entity.OrderItem;
import com.skala.shop.entity.Product;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // OrderItem의 customer 필드가 참조하는 Customer의 customerId와 일치하는 목록 조회 (속성 순회)
    List<OrderItem> findByCustomer_CustomerId(String customerId);

    // 특정 고객이 특정 상품을 이미 보유(주문)하고 있는지 검색
    Optional<OrderItem> findByCustomerAndProduct(Customer customer, Product product);

    // 고객 삭제 시 FK 제약 위반을 막기 위해 연관 주문 내역을 함께 정리
    void deleteByCustomer(Customer customer);

    // 상품 삭제 시 FK 제약 위반을 막기 위해 연관 주문 내역을 함께 정리
    void deleteByProduct(Product product);
}
