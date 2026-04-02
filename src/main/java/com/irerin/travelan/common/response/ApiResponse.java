package com.irerin.travelan.common.response;

import java.util.LinkedHashMap;
import java.util.Map;

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
    private final Object search;
    private final Map<String, Object> meta;
    private final ErrorResponse error;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null, null, null);
    }

    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(true, null, null, null, null, null);
    }

    public static <T> ApiResponse<T> error(ErrorResponse errorResponse) {
        return new ApiResponse<>(false, null, null, null, null, errorResponse);
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static final class Builder<T> {
        private T data;
        private PageMeta page;
        private Object search;
        private Map<String, Object> meta;
        private ErrorResponse error;
        private boolean success = true;

        private Builder() {}

        public Builder<T> data(T data)          { this.data = data; return this; }

        @SuppressWarnings("unchecked")
        public <E> Builder<T> data(Page<E> pageResult) {
            this.data = (T) pageResult.getContent();
            this.page = PageMeta.from(pageResult, pageResult.getNumber() + 1);
            return this;
        }

        public Builder<T> page(PageMeta page)   { this.page = page; return this; }
        public Builder<T> search(Object search) { this.search = search; return this; }
        public Builder<T> meta(String key, Object value) {
            if (this.meta == null) {
                this.meta = new LinkedHashMap<>();
            }
            this.meta.put(key, value);
            return this;
        }
        public Builder<T> error(ErrorResponse error) {
            this.error = error;
            this.success = false;
            return this;
        }

        public ApiResponse<T> build() {
            return new ApiResponse<>(success, data, page, search, meta, error);
        }
    }
}
