package com.irerin.travelan.common.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private ErrorResponse error;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.data = data;
        return response;
    }

    public static <T> ApiResponse<T> ok() {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        return response;
    }

    public static <T> ApiResponse<T> error(ErrorResponse errorResponse) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.error = errorResponse;
        return response;
    }
}
