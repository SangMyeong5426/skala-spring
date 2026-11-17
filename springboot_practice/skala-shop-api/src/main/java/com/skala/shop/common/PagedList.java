package com.skala.shop.common;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.Getter;

/**
 * 페이지 단위 목록 조회 결과를 감싸는 공통 객체.
 */
@Getter
public class PagedList<T> {

    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    private PagedList(List<T> content, int page, int size, long totalElements, int totalPages) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public static <T> PagedList<T> of(Page<T> page) {
        return new PagedList<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
