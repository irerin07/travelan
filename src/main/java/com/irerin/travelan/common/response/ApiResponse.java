package com.irerin.travelan.common.response;

import java.util.List;

import org.springframework.data.domain.Page;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final PageMeta page;
    private final ErrorResponse error;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(true, null, null, null);
    }

    public static <T> ApiResponse<List<T>> ofPage(Page<T> pageResult, int pageNumber) {
        return new ApiResponse<>(true, pageResult.getContent(), PageMeta.from(pageResult, pageNumber), null);
    }

    public static <T> ApiResponse<T> error(ErrorResponse errorResponse) {
        return new ApiResponse<>(false, null, null, errorResponse);
    }
}
