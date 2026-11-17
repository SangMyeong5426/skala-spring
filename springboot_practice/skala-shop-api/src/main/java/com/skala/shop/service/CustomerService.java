package com.skala.shop.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skala.shop.common.PagedList;
import com.skala.shop.common.Response;
import com.skala.shop.common.SessionHandler;
import com.skala.shop.dto.CustomerSession;
import com.skala.shop.dto.LoginResponseDto;
import com.skala.shop.dto.OrderItemDto;
import com.skala.shop.dto.OrderListDto;
import com.skala.shop.dto.OrderRequest;
import com.skala.shop.dto.PointHistoryDto;
import com.skala.shop.entity.Customer;
import com.skala.shop.entity.OrderItem;
import com.skala.shop.entity.PointHistory;
import com.skala.shop.entity.Product;
import com.skala.shop.exception.Error;
import com.skala.shop.exception.ParameterException;
import com.skala.shop.exception.ResponseException;
import com.skala.shop.metrics.ShopMetrics;
import com.skala.shop.repository.CustomerRepository;
import com.skala.shop.repository.OrderItemRepository;
import com.skala.shop.repository.PointHistoryRepository;
import com.skala.shop.repository.ProductRepository;
import com.skala.shop.tools.StringUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final OrderItemRepository orderItemRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final SessionHandler sessionHandler;
    private final PasswordEncoder passwordEncoder;
    private final ShopMetrics shopMetrics;

    @Value("${shop.initial-point}")
    private Double initialPoint;

    // 전체 고객 목록 조회
    @Transactional(readOnly = true)
    public Response<PagedList<Customer>> getAllCustomers(int offset, int count) {
        Pageable pageable = PageRequest.of(offset, count, Sort.by("customerId").ascending());
        Page<Customer> page = customerRepository.findAll(pageable);
        return Response.ok(PagedList.of(page));
    }

    // 단일 고객 상세 + 보유(주문) 상품 목록 조회
    @Transactional(readOnly = true)
    public Response<OrderListDto> getCustomerById(String customerId) {
        Customer customer = findCustomerOrThrow(customerId);
        List<OrderItemDto> products = toOrderItemDtos(customer);
        OrderListDto dto = OrderListDto.builder()
                .customerId(customer.getCustomerId())
                .customerPoint(customer.getCustomerPoint())
                .products(products)
                .build();
        return Response.ok(dto);
    }

    // 고객 생성 (회원가입) - 초기 포인트 지급
    @Transactional
    public Response<Customer> createCustomer(Customer input) {
        if (StringUtil.isAnyEmpty(input.getCustomerId(), input.getCustomerPassword())) {
            throw new ParameterException("customerId", "customerPassword");
        }
        if (customerRepository.existsById(input.getCustomerId())) {
            throw new ResponseException(Error.DATA_DUPLICATED, "이미 존재하는 고객 ID입니다: " + input.getCustomerId());
        }

        Customer customer = new Customer(input.getCustomerId(), initialPoint);
        customer.setCustomerPassword(passwordEncoder.encode(input.getCustomerPassword()));
        customerRepository.save(customer);

        recordPointHistory(customer, PointHistory.Reason.SIGNUP, initialPoint);

        return Response.ok("회원가입이 완료되었습니다. 초기 포인트가 지급되었습니다.", customer);
    }

    // 고객 로그인 - 성공 시 세션 토큰 발급
    @Transactional(readOnly = true)
    public Response<LoginResponseDto> loginCustomer(CustomerSession session) {
        if (StringUtil.isAnyEmpty(session.getCustomerId(), session.getCustomerPassword())) {
            throw new ParameterException("customerId", "customerPassword");
        }
        Customer customer = customerRepository.findById(session.getCustomerId())
                .orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND, "존재하지 않는 고객입니다."));

        if (!passwordEncoder.matches(session.getCustomerPassword(), customer.getCustomerPassword())) {
            throw new ResponseException(Error.NOT_AUTHENTICATED, "비밀번호가 일치하지 않습니다.");
        }

        String token = sessionHandler.issueToken(customer.getCustomerId());
        LoginResponseDto dto = LoginResponseDto.builder()
                .token(token)
                .customerId(customer.getCustomerId())
                .customerPoint(customer.getCustomerPoint())
                .build();
        return Response.ok("로그인 되었습니다.", dto);
    }

    // 로그아웃 - 세션 토큰 무효화 (편의 기능)
    public Response<Void> logout(String token) {
        sessionHandler.invalidate(token);
        return Response.ok("로그아웃 되었습니다.", null);
    }

    // 고객 정보 수정 (포인트)
    @Transactional
    public Response<Customer> updateCustomer(Customer input) {
        if (StringUtil.isEmpty(input.getCustomerId()) || input.getCustomerPoint() == null) {
            throw new ParameterException("customerId", "customerPoint");
        }
        Customer customer = findCustomerOrThrow(input.getCustomerId());
        customer.setCustomerPoint(input.getCustomerPoint());
        return Response.ok("고객 정보가 수정되었습니다.", customer);
    }

    // 고객 삭제 (FK 제약 위반 방지를 위해 연관된 주문/포인트 이력을 먼저 정리)
    @Transactional
    public Response<Void> deleteCustomer(String customerId) {
        Customer customer = findCustomerOrThrow(customerId);
        orderItemRepository.deleteByCustomer(customer);
        pointHistoryRepository.deleteByCustomer(customer);
        customerRepository.delete(customer);
        return Response.ok("고객 정보가 삭제되었습니다.", null);
    }

    // 상품 주문 (포인트 차감, 재고 차감)
    @Transactional
    public Response<OrderListDto> placeOrder(String customerId, OrderRequest order) {
        if (order.getProductId() == null || order.getQuantity() == null || order.getQuantity() <= 0) {
            throw new ParameterException("productId", "quantity");
        }

        Customer customer = findCustomerOrThrow(customerId);
        Product product = findProductOrThrow(order.getProductId());

        double cost = product.getProductPrice() * order.getQuantity();
        if (customer.getCustomerPoint() < cost) {
            throw new ResponseException(Error.INSUFFICIENT_FUNDS,
                    "보유 포인트(" + customer.getCustomerPoint() + ")가 주문 금액(" + cost + ")보다 부족합니다.");
        }
        if (product.getProductStock() < order.getQuantity()) {
            throw new ResponseException(Error.OUT_OF_STOCK,
                    "상품 재고(" + product.getProductStock() + ")가 주문 수량(" + order.getQuantity() + ")보다 부족합니다.");
        }

        customer.setCustomerPoint(customer.getCustomerPoint() - cost);
        product.setProductStock(product.getProductStock() - order.getQuantity());

        OrderItem item = orderItemRepository.findByCustomerAndProduct(customer, product)
                .orElseGet(() -> new OrderItem(customer, product, 0));
        item.setQuantity(item.getQuantity() + order.getQuantity());
        orderItemRepository.save(item);

        recordPointHistory(customer, PointHistory.Reason.ORDER, -cost);
        shopMetrics.recordOrder(cost);

        List<OrderItemDto> products = toOrderItemDtos(customer);
        OrderListDto dto = OrderListDto.builder()
                .customerId(customer.getCustomerId())
                .customerPoint(customer.getCustomerPoint())
                .products(products)
                .build();
        return Response.ok("주문이 완료되었습니다.", dto);
    }

    // 주문 취소 (포인트 환급, 재고 복원)
    @Transactional
    public Response<OrderListDto> cancelOrder(String customerId, OrderRequest order) {
        if (order.getProductId() == null || order.getQuantity() == null || order.getQuantity() <= 0) {
            throw new ParameterException("productId", "quantity");
        }

        Customer customer = findCustomerOrThrow(customerId);
        Product product = findProductOrThrow(order.getProductId());

        OrderItem item = orderItemRepository.findByCustomerAndProduct(customer, product)
                .orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND, "취소할 주문 내역이 없습니다."));

        if (item.getQuantity() < order.getQuantity()) {
            throw new ResponseException(Error.INSUFFICIENT_QUANTITY,
                    "보유 주문 수량(" + item.getQuantity() + ")보다 많은 수량을 취소할 수 없습니다.");
        }

        double refund = product.getProductPrice() * order.getQuantity();
        int remainingQuantity = item.getQuantity() - order.getQuantity();

        if (remainingQuantity == 0) {
            orderItemRepository.delete(item);
        } else {
            item.setQuantity(remainingQuantity);
            orderItemRepository.save(item);
        }

        customer.setCustomerPoint(customer.getCustomerPoint() + refund);
        product.setProductStock(product.getProductStock() + order.getQuantity());

        recordPointHistory(customer, PointHistory.Reason.CANCEL, refund);
        shopMetrics.recordCancel();

        List<OrderItemDto> products = toOrderItemDtos(customer);
        OrderListDto dto = OrderListDto.builder()
                .customerId(customer.getCustomerId())
                .customerPoint(customer.getCustomerPoint())
                .products(products)
                .build();
        return Response.ok("주문이 취소되었습니다.", dto);
    }

    // 포인트 변동 이력 조회 (차별화 기능)
    @Transactional(readOnly = true)
    public Response<List<PointHistoryDto>> getPointHistory(String customerId) {
        findCustomerOrThrow(customerId);
        List<PointHistoryDto> history = pointHistoryRepository.findByCustomer_CustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(h -> PointHistoryDto.builder()
                        .reason(h.getReason().name())
                        .changeAmount(h.getChangeAmount())
                        .balanceAfter(h.getBalanceAfter())
                        .createdAt(h.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
        return Response.ok(history);
    }

    private void recordPointHistory(Customer customer, PointHistory.Reason reason, Double changeAmount) {
        pointHistoryRepository.save(new PointHistory(customer, reason, changeAmount, customer.getCustomerPoint()));
    }

    private List<OrderItemDto> toOrderItemDtos(Customer customer) {
        return orderItemRepository.findByCustomer_CustomerId(customer.getCustomerId()).stream()
                .map(item -> OrderItemDto.builder()
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getProductName())
                        .productPrice(item.getProduct().getProductPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getProduct().getProductPrice() * item.getQuantity())
                        .build())
                .collect(Collectors.toList());
    }

    private Customer findCustomerOrThrow(String customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND, "고객을 찾을 수 없습니다. customerId=" + customerId));
    }

    private Product findProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND, "상품을 찾을 수 없습니다. id=" + productId));
    }
}
