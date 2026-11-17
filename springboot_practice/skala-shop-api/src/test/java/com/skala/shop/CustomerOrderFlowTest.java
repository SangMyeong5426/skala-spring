package com.skala.shop;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;

/**
 * 회원가입 -> 로그인 -> 주문 -> 취소로 이어지는 핵심 시나리오와
 * 인증/포인트/재고/권한 관련 비즈니스 규칙을 검증하는 통합 테스트.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CustomerOrderFlowTest {

    @Autowired
    private MockMvc mockMvc;

    // 회원가입(이미 있으면 무시) 후 로그인해서 세션 쿠키를 반환하는 테스트 헬퍼.
    private Cookie loginAs(String customerId, String password) throws Exception {
        mockMvc.perform(post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":\"" + customerId + "\",\"customerPassword\":\"" + password + "\"}"));
        MvcResult loginResult = mockMvc.perform(post("/api/customers/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"" + customerId + "\",\"customerPassword\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return loginResult.getResponse().getCookie("shop-access");
    }

    // 애플리케이션 기동 시 AdminAccountInitializer가 만들어두는 기본 관리자 계정으로 로그인.
    private Cookie loginAsAdmin() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/customers/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"admin\",\"customerPassword\":\"admin1234\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return loginResult.getResponse().getCookie("shop-access");
    }

    @Test
    void 회원가입_로그인_주문_취소_전체_흐름을_검증한다() throws Exception {
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"tester01\",\"customerPassword\":\"pw1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerPoint", is(1000000.0)));

        MvcResult loginResult = mockMvc.perform(post("/api/customers/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"tester01\",\"customerPassword\":\"pw1234\"}"))
                .andExpect(status().isOk())
                .andReturn();

        Cookie sessionCookie = loginResult.getResponse().getCookie("shop-access");

        // 로그인 없이 주문 -> 401 NOT_AUTHENTICATED
        mockMvc.perform(post("/api/customers/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"quantity\":1}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("NOT_AUTHENTICATED")));

        // 무선마우스(15000) 2개 주문 -> 970000 P 남음
        mockMvc.perform(post("/api/customers/order")
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerPoint", is(970000.0)));

        // 1개 취소 -> 985000 P 환급
        mockMvc.perform(post("/api/customers/cancel")
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"quantity\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerPoint", is(985000.0)))
                .andExpect(jsonPath("$.data.products[0].quantity", is(1)));

        // 보유 포인트를 초과하는 고가 상품 대량 주문 -> INSUFFICIENT_FUNDS
        mockMvc.perform(post("/api/customers/order")
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":4,\"quantity\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INSUFFICIENT_FUNDS")));

        // 보유 수량(1개)보다 많은 취소 요청 -> INSUFFICIENT_QUANTITY
        mockMvc.perform(post("/api/customers/cancel")
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"quantity\":99}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INSUFFICIENT_QUANTITY")));

        mockMvc.perform(delete("/api/customers/tester01"))
                .andExpect(status().isOk());
    }

    @Test
    void 존재하지_않는_상품_조회시_DATA_NOT_FOUND를_반환한다() throws Exception {
        mockMvc.perform(get("/api/products/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("DATA_NOT_FOUND")));
    }

    @Test
    void 필수값이_없으면_PARAMETER_ERROR를_반환한다() throws Exception {
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("PARAMETER_ERROR")));
    }

    // 명세서의 API 목록 표(루트 경로)와 Controller 상세 절(@GetMapping("/list"))이 서로 다른 경로를
    // 제시하고 있어, 두 경로를 모두 지원하도록 만든 부분에 대한 회귀 테스트.
    @Test
    void 목록_조회는_루트_경로와_list_경로를_모두_지원한다() throws Exception {
        mockMvc.perform(get("/api/products")).andExpect(status().isOk());
        mockMvc.perform(get("/api/products/list")).andExpect(status().isOk());
        mockMvc.perform(get("/api/customers")).andExpect(status().isOk());
        mockMvc.perform(get("/api/customers/list")).andExpect(status().isOk());
    }

    // 주문 이력이 남아있는 상품을 관리자가 삭제해도 FK 제약 위반(500) 없이 정상 삭제되어야 한다.
    @Test
    void 주문_이력이_있는_상품도_정상적으로_삭제된다() throws Exception {
        Cookie adminCookie = loginAsAdmin();

        MvcResult createResult = mockMvc.perform(post("/api/products")
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\":\"삭제테스트상품\",\"productPrice\":1000,\"productStock\":10}"))
                .andExpect(status().isOk())
                .andReturn();
        Number productIdNum = com.jayway.jsonpath.JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.id");

        Cookie customerCookie = loginAs("delTester", "pw1234");

        mockMvc.perform(post("/api/customers/order")
                        .cookie(customerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productIdNum + ",\"quantity\":1}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/products")
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + productIdNum + "}"))
                .andExpect(status().isOk());
    }

    // 관리자/고객 역할 분리 (차별화 포인트) - 일반 고객은 상품 쓰기 API를 호출할 수 없다.
    @Test
    void 일반_고객은_상품을_등록할_수_없고_FORBIDDEN을_반환한다() throws Exception {
        Cookie customerCookie = loginAs("roleTester", "pw1234");

        mockMvc.perform(post("/api/products")
                        .cookie(customerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\":\"권한테스트상품\",\"productPrice\":1000,\"productStock\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("FORBIDDEN")));

        // 로그인조차 하지 않은 경우는 NOT_AUTHENTICATED
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\":\"권한테스트상품2\",\"productPrice\":1000,\"productStock\":1}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("NOT_AUTHENTICATED")));
    }

    // 관리자 계정은 상품을 등록/수정할 수 있어야 한다.
    @Test
    void 관리자는_상품을_등록하고_수정할_수_있다() throws Exception {
        Cookie adminCookie = loginAsAdmin();

        MvcResult createResult = mockMvc.perform(post("/api/products")
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\":\"관리자등록상품\",\"productPrice\":5000,\"productStock\":3}"))
                .andExpect(status().isOk())
                .andReturn();
        Number productIdNum = com.jayway.jsonpath.JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/products")
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + productIdNum + ",\"productName\":\"관리자수정상품\",\"productPrice\":6000,\"productStock\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productName", is("관리자수정상품")));
    }
}
