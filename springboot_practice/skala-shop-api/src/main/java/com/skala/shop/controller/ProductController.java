package com.skala.shop.controller;

import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skala.shop.common.OptimisticRetry;
import com.skala.shop.common.PagedList;
import com.skala.shop.common.Response;
import com.skala.shop.common.SessionHandler;
import com.skala.shop.entity.Product;
import com.skala.shop.service.AuthService;
import com.skala.shop.service.ProductService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final AuthService authService;

    // 전체 상품 목록 조회 (페이징 + 선택적 이름 검색) - 로그인 없이 누구나 조회 가능
    // 명세서 내 API 목록 표(GET /api/products)와 Controller 상세 절(@GetMapping("/list"))이
    // 서로 다른 경로를 제시하고 있어, 두 경로를 모두 지원하도록 처리했다.
    @GetMapping({"", "/list"})
    public Response<PagedList<Product>> getAllProducts(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int count,
            @RequestParam(required = false) String keyword) {
        return productService.getAllProducts(offset, count, keyword);
    }

    // 개별 상품 상세 조회 - 로그인 없이 누구나 조회 가능
    @GetMapping("/{id}")
    public Response<Product> getProductById(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    // 상품 등록 - 관리자 전용 (관리자/고객 역할 분리 - 차별화 포인트)
    @PostMapping
    public Response<Product> createProduct(@CookieValue(value = SessionHandler.COOKIE_NAME, required = false) String token,
                                            @RequestBody Product product) {
        authService.requireAdmin(token);
        return productService.createProduct(product);
    }

    // 상품 정보 수정 - 관리자 전용 (동시 재고 수정 충돌 시 자동 재시도)
    @PutMapping
    public Response<Product> updateProduct(@CookieValue(value = SessionHandler.COOKIE_NAME, required = false) String token,
                                            @RequestBody Product product) {
        authService.requireAdmin(token);
        return OptimisticRetry.execute(() -> productService.updateProduct(product));
    }

    // 상품 삭제 - 관리자 전용
    @DeleteMapping
    public Response<Void> deleteProduct(@CookieValue(value = SessionHandler.COOKIE_NAME, required = false) String token,
                                         @RequestBody Product product) {
        authService.requireAdmin(token);
        return productService.deleteProduct(product);
    }
}
