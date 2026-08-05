package com.example.menu.controller;

import com.example.menu.dto.MenuResponse;
import com.example.menu.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 브라우저의 HTTP 요청을 받아 메뉴 추천 결과를 반환한다.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "메뉴 추천", description = "조건에 맞는 메뉴를 추천하는 API")
public class MenuController {

    private final MenuService menuService;

    /**
     * Spring Container가 MenuService Bean을 생성자에 주입합니다.
     */
    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @Operation(summary = "사용자에게 인사하기")
    @GetMapping("/hello/{name}")
    public String hello(@PathVariable("name") String name) {
        return name + "님, 오늘도 맛있는 하루 보내세요!";
    }

    @Operation(summary = "기본 메뉴 추천")
    @GetMapping("/menu")
    public String menu() {
        return "오늘의 추천 메뉴는 " + menuService.recommend() + "입니다.";
    }

    @Operation(summary = "무작위 메뉴 추천")
    @GetMapping("/menu/random")
    public String randomMenu() {
        return "오늘은 " + menuService.randomMenu() + " 어떠세요?";
    }

    @Operation(summary = "카테고리별 메뉴 추천")
    @GetMapping("/menu/{category}")
    public String menuByCategory(@PathVariable("category") String category) {
        String menu = menuService.recommendByCategory(category);
        return category + " 추천 메뉴는 " + menu + "입니다.";
    }

    @Operation(summary = "카테고리별 메뉴를 JSON으로 추천")
    @GetMapping("/menu/json/{category}")
    public MenuResponse menuJson(@PathVariable("category") String category) {
        String menu = menuService.recommendByCategory(category);

        return new MenuResponse(
                category,
                menu,
                "오늘은 " + menu + " 어떠세요?"
        );
    }

    @Operation(summary = "날씨별 메뉴 추천")
    @GetMapping("/menu/weather/{weather}")
    public String menuByWeather(@PathVariable("weather") String weather) {
        String menu = menuService.recommendByWeather(weather);
        return weather + " 날씨 추천 메뉴는 " + menu + "입니다.";
    }

    @Operation(summary = "기분별 메뉴 추천 (JSON)")
    @GetMapping("/menu/mood/{mood}")
    public MenuResponse menuByMood(@PathVariable("mood") String mood) {
        String menu = menuService.recommendByMood(mood);

        return new MenuResponse(
                mood,
                menu,
                "기분이 " + mood + "이라면 " + menu + " 어떠세요?"
        );
    }

    @Operation(summary = "가격대별 메뉴 추천")
    @GetMapping("/menu/price/search")
    public String menuByPrice(@RequestParam("min") int min, @RequestParam("max") int max) {
        String menu = menuService.recommendByPrice(min, max);
        return min + "원~" + max + "원 추천 메뉴는 " + menu + "입니다.";
    }

    @Operation(summary = "지역별 메뉴 추천")
    @GetMapping("/menu/my/{region}")
    public String menuByRegion(@PathVariable("region") String region) {
        String menu = menuService.recommendByRegion(region);
        return region + " 지역 추천 메뉴는 " + menu + "입니다.";
    }
}
