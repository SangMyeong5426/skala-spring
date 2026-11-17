# SKALA-SHOP API

상품 · 고객 · 주문(상품)을 관리하는 온라인 쇼핑몰 백엔드 REST API.
Controller → Service → Repository 계층형 구조로 구현했다.

## 기술 스택

- Spring Boot 3.3.0 / Java 17
- Spring Data JPA + H2 (in-memory)
- JPA `@Version` 낙관적 락 + 자동 재시도 (동시성 제어)
- 관리자/고객 역할(Role) 분리
- Spring Boot AOP, Actuator (+ Micrometer 커스텀 메트릭)
- spring-security-crypto (BCrypt 비밀번호 해싱만 단독 사용)
- springdoc-openapi (Swagger UI)
- Gradle
- 프론트엔드: 순수 HTML/CSS/JS (Spring Boot 정적 리소스로 서빙)

## 실행 방법

```bash
./gradlew bootRun
```

- 웹 UI: http://localhost:8080/
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- H2 콘솔: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:shopdb`, user: `sa`)
- Actuator: http://localhost:8080/actuator/health , /actuator/info , /actuator/metrics

기동 시 기본 관리자 계정(`admin` / `admin1234`)이 자동 생성된다. 상품 등록/수정/삭제는
이 계정으로 로그인해야 호출할 수 있다.

## 인증 및 인가 방식

- **로그인 (JWT 미사용)**: 로그인 성공 시 서버가 UUID 기반 opaque 세션 토큰을 발급해
  `shop-access`라는 이름의 HttpOnly Cookie로 내려준다. 이후 주문/취소 요청은 이 쿠키에서
  토큰을 읽어 로그인된 고객을 식별한다 (`SessionHandler`가 토큰↔고객ID 매핑을 서버 메모리에서
  관리). JWT 라이브러리 없이도 "로그인 필수" 요구사항을 충족하도록 설계했다.
- **인가 (관리자/고객 역할 분리)**: `Customer.role`이 `CUSTOMER`/`ADMIN`을 구분하며, 회원가입
  API는 항상 `CUSTOMER`만 생성한다. 상품 등록/수정/삭제는 `AuthService.requireAdmin()`이
  로그인 여부와 역할을 함께 검사해 관리자만 호출할 수 있다 (아니면 401/403).

## 동시성 제어

`Product`, `Customer`, `OrderItem`에 JPA `@Version`(낙관적 락)을 적용했다. 여러 요청이 동시에
같은 행(재고, 포인트 등)을 수정하면 나중에 커밋하는 쪽이 실패하는데, 이를 클라이언트에 그대로
노출하지 않고 `OptimisticRetry`가 짧은 지터를 두고 최대 10회까지 자동 재시도한다. 재시도를 모두
소진해야만 `CONCURRENT_CONFLICT`(409)를 반환한다. 재고 5개짜리 상품에 8명이 동시에 주문해도
정확히 5건만 성공하고 초과 판매가 발생하지 않는 것을 `OrderConcurrencyTest`로 검증한다.

## API 요약

| 메서드 | URI | 설명 |
|---|---|---|
| GET | /api/products | 상품 목록 (페이징 + `keyword` 검색) |
| GET | /api/products/{id} | 상품 상세 |
| POST | /api/products | 상품 등록 **(관리자 전용)** |
| PUT | /api/products | 상품 수정 **(관리자 전용)** |
| DELETE | /api/products | 상품 삭제 **(관리자 전용)** |
| GET | /api/customers | 고객 목록 |
| GET | /api/customers/{customerId} | 고객 상세 + 보유 주문 목록 |
| GET | /api/customers/{customerId}/products | 고객의 주문 상품 목록 |
| GET | /api/customers/{customerId}/points | 포인트 변동 이력 (차별화) |
| POST | /api/customers | 회원가입 (초기 포인트 지급) |
| POST | /api/customers/login | 로그인 (세션 쿠키 발급) |
| POST | /api/customers/logout | 로그아웃 |
| PUT | /api/customers | 고객 정보(포인트) 수정 |
| DELETE | /api/customers/{customerId} | 고객 삭제 |
| POST | /api/customers/order | 상품 주문 (로그인 필수, 동시성 재시도 적용) |
| POST | /api/customers/cancel | 주문 취소 (로그인 필수, 동시성 재시도 적용) |

## 비즈니스 규칙

- 주문 시 포인트 부족 → `INSUFFICIENT_FUNDS`, 재고 부족 → `OUT_OF_STOCK`
- 같은 상품 재주문 시 수량 누적, 취소 시 수량 차감(0이면 삭제)
- 취소 가능 수량 초과 → `INSUFFICIENT_QUANTITY`
- 관리자가 아닌 계정의 상품 쓰기 요청 → `FORBIDDEN`
- 낙관적 락 재시도 소진 → `CONCURRENT_CONFLICT`
- 없는 데이터 조회 → `DATA_NOT_FOUND` (전역 예외 처리)
- 필수값 누락 → `ParameterException` → 400
- 주문/취소는 `@Transactional`로 원자적 처리

## Actuator 커스텀 메트릭

`ShopMetrics`가 Micrometer `Counter`로 `shop.orders.count`(주문 건수), `shop.orders.cancelled.count`
(취소 건수), `shop.revenue.total`(누적 매출)을 등록한다. `/actuator/metrics/shop.orders.count`처럼
개별 조회할 수 있다.

## 테스트

```bash
./gradlew test
```

`CustomerOrderFlowTest`(7개) + `OrderConcurrencyTest`(1개, 8스레드 동시 주문으로 초과 판매 방지 검증) 총 8개.
