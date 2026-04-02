package com.irerin.travelan.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.irerin.travelan.common.response.ApiResponse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleException_500_에러_반환() {
        ResponseEntity<ApiResponse<?>> response = handler.handleException(new RuntimeException("unexpected"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getError().getCode()).isEqualTo("INTERNAL_ERROR");
    }

    @Test
    void handleDataIntegrityViolation_409_반환() {
        ResponseEntity<ApiResponse<?>> response = handler.handleDataIntegrityViolation(
            new DataIntegrityViolationException("duplicate key")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getError().getCode()).isEqualTo("DUPLICATE");
    }

    @Test
    void handleAuth_401_반환() {
        ResponseEntity<ApiResponse<?>> response = handler.handleAuth(
            new AuthException("인증 실패")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getError().getCode()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    void handleDuplicate_409_반환() {
        ResponseEntity<ApiResponse<?>> response = handler.handleDuplicate(
            new DuplicateException("중복")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getError().getCode()).isEqualTo("DUPLICATE");
    }
}
