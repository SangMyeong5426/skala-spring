package com.example.menu.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI menuOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("오늘 뭐 먹지 API")
                        .description("메뉴 추천 Spring Boot 실습 API")
                        .version("v1"));
    }
}
