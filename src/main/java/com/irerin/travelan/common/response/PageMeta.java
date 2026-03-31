package com.irerin.travelan.common.response;

import org.springframework.data.domain.Page;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PageMeta {

    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    public static PageMeta from(Page<?> pageResult, int pageNumber) {
        return new PageMeta(
            pageNumber,
            pageResult.getSize(),
            pageResult.getTotalElements(),
            pageResult.getTotalPages()
        );
    }
}
