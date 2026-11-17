package com.skala.shop.common;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * JWT 없이 로그인 필수 요건을 충족하기 위한 경량 세션 저장소.
 * 로그인 성공 시 발급한 opaque 토큰(UUID)을 Cookie로 내려주고, 이후 요청은
 * 해당 토큰으로 로그인된 customerId를 조회하여 인증 여부를 판단한다.
 */
@Component
public class SessionHandler {

    public static final String COOKIE_NAME = "shop-access";

    private final Map<String, String> tokenStore = new ConcurrentHashMap<>();

    public String issueToken(String customerId) {
        String token = UUID.randomUUID().toString();
        tokenStore.put(token, customerId);
        return token;
    }

    public String resolveCustomerId(String token) {
        if (token == null) {
            return null;
        }
        return tokenStore.get(token);
    }

    public void invalidate(String token) {
        if (token != null) {
            tokenStore.remove(token);
        }
    }
}
