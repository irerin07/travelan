package com.irerin.travelan.common.response;

import org.springframework.data.domain.Page;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class PageMeta {

    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    @Builder(access = AccessLevel.PRIVATE)
    private PageMeta(int page, int size, long totalElements, int totalPages) {
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public static PageMeta from(Page<?> pageResult, int pageNumber) {
        return PageMeta.builder()
            .page(pageNumber)
            .size(pageResult.getSize())
            .totalElements(pageResult.getTotalElements())
            .totalPages(pageResult.getTotalPages())
            .build();
    }
}
