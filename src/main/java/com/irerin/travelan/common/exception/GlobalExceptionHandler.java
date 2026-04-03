package com.irerin.travelan.common.exception;

import java.util.List;

import jakarta.validation.ConstraintViolationException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import com.irerin.travelan.common.response.ApiResponse;
import com.irerin.travelan.common.response.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> ErrorResponse.FieldError.of(e.getField(), e.getDefaultMessage()))
            .toList();

        return ResponseEntity.badRequest()
            .body(ApiResponse.error(ErrorResponse.of("VALIDATION_ERROR", "입력값이 올바르지 않습니다", fieldErrors)));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodValidation(HandlerMethodValidationException ex) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getValueResults().stream()
            .flatMap(result -> {
                String paramName = result.getMethodParameter().getParameterName();
                return result.getResolvableErrors().stream()
                    .map(error -> ErrorResponse.FieldError.of(paramName, error.getDefaultMessage()));
            })
            .toList();

        return ResponseEntity.badRequest()
            .body(ApiResponse.error(ErrorResponse.of("VALIDATION_ERROR", "입력값이 올바르지 않습니다", fieldErrors)));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleConstraintViolation(ConstraintViolationException ex) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations().stream()
            .map(v -> {
                String path = v.getPropertyPath().toString();
                String fieldName = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
                return ErrorResponse.FieldError.of(fieldName, v.getMessage());
            })
            .toList();

        return ResponseEntity.badRequest()
            .body(ApiResponse.error(ErrorResponse.of("VALIDATION_ERROR", "입력값이 올바르지 않습니다", fieldErrors)));
    }

    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<ApiResponse<?>> handleDuplicate(DuplicateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(ErrorResponse.of("DUPLICATE", ex.getMessage())));
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<?>> handleAuth(AuthException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(ErrorResponse.of("UNAUTHORIZED", ex.getMessage())));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String message = resolveDataIntegrityMessage(ex);

        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(ErrorResponse.of("DUPLICATE", message)));
    }

    private String resolveDataIntegrityMessage(DataIntegrityViolationException ex) {
        String causeMessage = ex.getMostSpecificCause().getMessage();
        if (causeMessage != null) {
            String lower = causeMessage.toLowerCase();
            if (lower.contains("email")) {
                return "이미 사용 중인 이메일입니다";
            }
            if (lower.contains("phone")) {
                return "이미 사용 중인 휴대폰 번호입니다";
            }
            if (lower.contains("nickname")) {
                return "이미 사용 중인 닉네임입니다";
            }
        }

        return "데이터 무결성 제약 조건 위반입니다";
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception ex) {
        log.error("Unhandled exception", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(ErrorResponse.of("INTERNAL_ERROR", "서버 오류가 발생했습니다")));
    }

}
