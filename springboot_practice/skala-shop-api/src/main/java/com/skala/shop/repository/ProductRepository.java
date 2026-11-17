package com.skala.shop.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.skala.shop.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 상품 이름과 동일한 상품 데이터를 조회 (존재 여부 확인 목적 -> Optional)
    Optional<Product> findByProductName(String productName);

    // 상품 이름 부분 검색 (차별화: 검색 기능)
    Page<Product> findByProductNameContainingIgnoreCase(String keyword, Pageable pageable);
}
