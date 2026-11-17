# skala-spring

SKALA 과정에서 진행한 Spring Boot 실습 코드를 모은 저장소다.
메뉴 추천 API로 Bean 등록과 의존성 주입을 익히는 예제에서 시작해, H2 기반 주식 거래 API를 두 차례에 걸쳐 만들고,
상품·고객·주문을 다루는 쇼핑몰 API로 마무리했다. 폴더마다 독립된 Gradle 프로젝트이므로 해당 폴더로 이동해서 실행한다.

## 폴더 구성

| 폴더 | 내용 |
| --- | --- |
| [`springboot-menu-recommendation/`](springboot-menu-recommendation/) | "오늘 뭐 먹지?" 메뉴 추천 API. Component Scan, 생성자 주입, `@PathVariable` 실습과 추천 API 과제 |
| [`StockTrading/`](StockTrading/) | 주식 거래 API 2일차 코드. JPA + H2로 사용자·주식·거래·포트폴리오 API를 구성 |
| [`StockTrading2/`](StockTrading2/) | 주식 거래 API 3일차 과제. CRUD 보완, 매매 실행, 분석 API, JPA·MyBatis 병행, AOP 감사 로그, Actuator |
| [`springboot_practice/skala-shop-api/`](springboot_practice/skala-shop-api/) | SKALA-SHOP 쇼핑몰 REST API. 역할 분리, 낙관적 락 재시도, 커스텀 메트릭, 정적 웹 화면 |

| 프로젝트 | Spring Boot | Java | DB | Gradle 래퍼 |
| --- | --- | --- | --- | --- |
| springboot-menu-recommendation | 3.4.5 | 21 | 없음 | 포함 |
| StockTrading | 3.2.0 | 21 | H2 (메모리) | 미포함 |
| StockTrading2 | 3.2.0 | 21 | H2 (메모리) | 포함 |
| skala-shop-api | 3.3.0 | 17 | H2 (메모리) | 포함 |

네 프로젝트 모두 8080 포트를 쓴다. H2를 쓰는 프로젝트는 기동할 때마다 스키마를 새로 만들고 `data.sql`로 초기 데이터를 넣는다.

## springboot-menu-recommendation

학습 내용과 접속 주소는 [README](springboot-menu-recommendation/README.md), 과제 설명은 [실습과제.md](springboot-menu-recommendation/실습과제.md)에 있다.

```bash
cd springboot-menu-recommendation
./gradlew bootRun
```

- 시작 화면: http://localhost:8080
- 기본 API: `GET /api/hello/{name}`, `GET /api/menu`, `GET /api/menu/random`, `GET /api/menu/{category}`, `GET /api/menu/json/{category}`
- 과제로 추가한 API: `GET /api/menu/weather/{weather}`, `GET /api/menu/mood/{mood}`, `GET /api/menu/price/search?min=&max=`, `GET /api/menu/my/{region}`

## StockTrading (2일차)

[`StockTrading/README.md`](StockTrading/README.md)는 강의 배포 당시의 안내문(`day2` 폴더 기준)이다.
이 폴더에는 `gradlew` 스크립트가 들어 있지 않으므로 로컬에 설치한 Gradle로 실행한다.

```bash
cd StockTrading
gradle bootRun
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 콘솔: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:stockdb`, 접속 정보는 `application.yml` 설정값)
- 초기 데이터: 샘플 사용자 3명, 종목 5개

| 리소스 | 엔드포인트 |
| --- | --- |
| 사용자 | `POST /api/users`, `GET /api/users`, `GET /api/users/{id}` |
| 주식 | `POST /api/stocks`, `GET /api/stocks`, `GET /api/stocks/{id}` |
| 거래 | `POST /api/transactions/trade`, `GET /api/transactions/{id}`, `GET /api/transactions/user/{userId}` |
| 포트폴리오 | `GET /api/portfolios/user/{userId}`, `GET /api/portfolios/user/{userId}/stock/{stockId}` |

## StockTrading2 (3일차 과제)

요구사항 대비 구현 내역은 [`StockTrading2/docs/submission.html`](StockTrading2/docs/submission.html)에 정리했다.

```bash
cd StockTrading2
./gradlew bootRun
```

- Swagger UI와 H2 콘솔 주소는 StockTrading과 같다.
- Actuator: `/actuator/health`, `/actuator/info`, `/actuator/metrics`

StockTrading 대비 바뀐 점은 다음과 같다.

- CRUD 보완: `DELETE /api/users/{id}`, `GET /api/stocks/code/{code}`, `PUT /api/stocks/{id}`, `DELETE /api/stocks/{id}`, `GET /api/transactions/user/{userId}/stock/{stockId}`
- 분석 API(`/api/analysis`): 평가 손익(`/portfolio/{userId}/profit-loss`), 거래 상세(`/transactions/{id}`), 종목별 거래 내역(`/transactions/stock/{stockId}`), 총 자산(`/assets/{userId}`), 총 수익률(`/return-rate/{userId}`), 거래 통계(`/statistics/{userId}`), 일별 거래 내역(`/transactions/{userId}/daily`)
- 데이터 접근: 기본 CRUD는 JPA, 집계성 조회 일부는 MyBatis 매퍼(`src/main/resources/mapper/StockMapper.xml`)로 처리
- AOP: `TradeAuditAspect`가 `TransactionService.executeTrade(..)`를 감싸 실행 시간을 로그로 남기고, 성공·실패 결과를 `TradeAuditLog`에 저장한다. 저장은 별도 트랜잭션(`REQUIRES_NEW`)이라 매매가 롤백돼도 감사 로그는 남는다.
- 예외 처리: `GlobalExceptionHandler`가 입력 검증 오류와 런타임 예외를 응답으로 변환

## skala-shop-api

기술 스택, 인증·인가 방식, 동시성 제어, 전체 API 표는 [README](springboot_practice/skala-shop-api/README.md)에,
제출 문서는 [`docs/submission.html`](springboot_practice/skala-shop-api/docs/submission.html), 화면 캡처는 [`docs/screenshots/`](springboot_practice/skala-shop-api/docs/screenshots/)에 있다.

```bash
cd springboot_practice/skala-shop-api
./gradlew bootRun
./gradlew test
```

- 웹 화면: http://localhost:8080/ (정적 HTML/CSS/JS)
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- 주요 내용: 세션 쿠키 기반 로그인, 관리자/고객 역할 분리, JPA `@Version` 낙관적 락과 자동 재시도, 포인트 변동 이력, Micrometer 커스텀 메트릭, AOP API 로그
- 기동 시 관리자 계정이 자동으로 만들어진다(`AdminAccountInitializer`). 계정 정보는 하위 README를 참고한다.

## 저장소에서 제외한 파일

루트 `.gitignore`는 다음을 추적하지 않는다.

- 빌드·IDE 산출물: `.gradle/`, `build/`, `bin/`, `.vscode/`, `.DS_Store`
- 제출용 PDF(`StockTrading2/*.pdf`, `springboot_practice/**/*.pdf`)와 압축본(`*.zip`)
- `springboot_practice/` 아래 curl 쿠키 파일(`*.txt`, 세션 값이 들어 있음)과 Postman 로컬 설정
- 제출용으로 복사해 둔 폴더(`skala-shop-api`와 내용이 같음)
