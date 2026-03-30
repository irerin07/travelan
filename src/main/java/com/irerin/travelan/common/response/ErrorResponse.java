package com.irerin.travelan.common.response;

import java.util.List;

import lombok.Getter;

@Getter
public class ErrorResponse {

    private final String code;
    private final String message;
    private final List<FieldError> errors;

    public ErrorResponse(String code, String message) {
        this(code, message, List.of());
    }

    public ErrorResponse(String code, String message, List<FieldError> errors) {
        this.code = code;
        this.message = message;
        this.errors = errors;
    }

    @Getter
    public static class FieldError {
        private final String field;
        private final String message;

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }
    }
}
