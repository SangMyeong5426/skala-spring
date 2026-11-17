package com.skala.shop.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skala.shop.common.PagedList;
import com.skala.shop.common.Response;
import com.skala.shop.entity.Product;
import com.skala.shop.exception.Error;
import com.skala.shop.exception.ParameterException;
import com.skala.shop.exception.ResponseException;
import com.skala.shop.repository.OrderItemRepository;
import com.skala.shop.repository.ProductRepository;
import com.skala.shop.tools.StringUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    // 전체 상품 목록 조회 (페이징, 선택적 이름 검색)
    @Transactional(readOnly = true)
    public Response<PagedList<Product>> getAllProducts(int offset, int count, String keyword) {
        Pageable pageable = PageRequest.of(offset, count, Sort.by("id").ascending());
        Page<Product> page = StringUtil.isEmpty(keyword)
                ? productRepository.findAll(pageable)
                : productRepository.findByProductNameContainingIgnoreCase(keyword, pageable);
        return Response.ok(PagedList.of(page));
    }

    // 개별 상품 상세 조회
    @Transactional(readOnly = true)
    public Response<Product> getProductById(Long id) {
        Product product = findProductOrThrow(id);
        return Response.ok(product);
    }

    // 상품 등록
    @Transactional
    public Response<Product> createProduct(Product product) {
        if (StringUtil.isEmpty(product.getProductName()) || product.getProductPrice() == null
                || product.getProductPrice() <= 0) {
            throw new ParameterException("productName", "productPrice");
        }
        productRepository.findByProductName(product.getProductName()).ifPresent(p -> {
            throw new ResponseException(Error.DATA_DUPLICATED, "이미 등록된 상품명입니다: " + product.getProductName());
        });

        // 신규 등록은 JPA가 자동 생성하도록 id/version을 비워둔다.
        // (참고: @Version 도입 전에는 id=0L로 세팅했지만, 낙관적 락이 추가되면서 id는 non-null인데
        //  version은 null인 상태가 Hibernate에는 "detached인데 버전이 없는" 모순으로 해석되어
        //  PropertyValueException이 발생한다. null로 두어야 신규 엔터티로 정상 인식된다.)
        product.setId(null);
        if (product.getProductStock() == null) {
            product.setProductStock(0);
        }
        Product saved = productRepository.save(product);
        return Response.ok("상품이 등록되었습니다.", saved);
    }

    // 상품 정보 수정
    @Transactional
    public Response<Product> updateProduct(Product product) {
        if (StringUtil.isEmpty(product.getProductName()) || product.getProductPrice() == null
                || product.getProductPrice() <= 0) {
            throw new ParameterException("productName", "productPrice");
        }
        Product existing = findProductOrThrow(product.getId());
        existing.setProductName(product.getProductName());
        existing.setProductPrice(product.getProductPrice());
        if (product.getProductStock() != null) {
            existing.setProductStock(product.getProductStock());
        }
        return Response.ok("상품 정보가 수정되었습니다.", existing);
    }

    // 상품 삭제 (FK 제약 위반 방지를 위해 연관된 주문 내역을 먼저 정리)
    @Transactional
    public Response<Void> deleteProduct(Product product) {
        Product existing = findProductOrThrow(product.getId());
        orderItemRepository.deleteByProduct(existing);
        productRepository.delete(existing);
        return Response.ok("상품이 삭제되었습니다.", null);
    }

    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND, "상품을 찾을 수 없습니다. id=" + id));
    }
}
