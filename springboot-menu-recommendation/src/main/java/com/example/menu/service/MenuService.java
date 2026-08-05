package com.example.menu.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 메뉴 추천 비즈니스 로직을 담당하는 Spring Bean입니다.
 *
 * @Service를 사용하면 Component Scan을 통해 Spring Container에
 * 자동으로 Bean으로 등록됩니다.
 */
@Service
public class MenuService {

    private final List<String> menus = List.of(
            "김치찌개",
            "불고기",
            "짜장면",
            "돈가스",
            "떡볶이",
            "치킨",
            "피자"
    );

    public String recommend() {
        return "김치찌개";
    }

    public String recommendByCategory(String category) {
        return switch (category) {
            case "korean" -> "불고기";
            case "chinese" -> "짜장면";
            case "japanese" -> "돈가스";
            case "snack" -> "떡볶이";
            default -> "추천 가능한 메뉴가 없습니다";
        };
    }

    public String randomMenu() {
        int index = ThreadLocalRandom.current().nextInt(menus.size());
        return menus.get(index);
    }

    public String recommendByWeather(String weather) {
        List<String> candidates = switch (weather) {
            case "sunny" -> List.of("냉모밀", "샐러드", "김밥");
            case "rainy" -> List.of("파전", "짜장면", "라면");
            case "hot" -> List.of("콩국수", "냉면", "빙수");
            case "cold" -> List.of("국밥", "떡국", "어묵탕");
            default -> null;
        };
        return pickRandom(candidates);
    }

    public String recommendByMood(String mood) {
        List<String> candidates = switch (mood) {
            case "happy" -> List.of("피자", "초밥", "아이스크림");
            case "sad" -> List.of("떡볶이", "마라탕", "초콜릿케이크");
            case "tired" -> List.of("삼계탕", "갈비탕", "죽");
            case "stressed" -> List.of("치킨", "매운제육볶음", "곱창");
            default -> null;
        };
        return pickRandom(candidates);
    }

    public String recommendByPrice(int min, int max) {
        if (min > max) {
            return "가격 범위가 올바르지 않습니다. (min은 max보다 클 수 없습니다)";
        }
        List<String> candidates;
        if (max <= 6000) {
            candidates = List.of("김밥", "라면", "편의점 도시락");
        } else if (max <= 12000) {
            candidates = List.of("돈가스", "제육볶음", "파스타");
        } else {
            candidates = List.of("스테이크", "오마카세", "한우 특수부위");
        }
        return pickRandom(candidates);
    }

    public String recommendByRegion(String region) {
        List<String> candidates = switch (region) {
            case "chuncheon" -> List.of("닭갈비", "막국수", "감자옹심이");
            case "busan" -> List.of("밀면", "돼지국밥", "씨앗호떡");
            case "jeonju" -> List.of("비빔밥", "콩나물국밥", "모주");
            case "jeju" -> List.of("흑돼지", "고기국수", "갈치조림");
            default -> null;
        };
        return pickRandom(candidates);
    }

    private String pickRandom(List<String> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return "추천 가능한 메뉴가 없습니다";
        }
        int index = ThreadLocalRandom.current().nextInt(candidates.size());
        return candidates.get(index);
    }
}
