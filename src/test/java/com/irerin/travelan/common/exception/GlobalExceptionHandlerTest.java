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
    void handleDataIntegrityViolation_이메일_제약조건_구체적_메시지_반환() {
        var cause = new RuntimeException("Duplicate entry 'test@test.com' for key 'users.email'");
        var ex = new DataIntegrityViolationException("could not execute statement", cause);

        ResponseEntity<ApiResponse<?>> response = handler.handleDataIntegrityViolation(ex);

        assertThat(response.getBody().getError().getMessage()).isEqualTo("이미 사용 중인 이메일입니다");
    }

    @Test
    void handleDataIntegrityViolation_전화번호_제약조건_구체적_메시지_반환() {
        var cause = new RuntimeException("Duplicate entry '01012345678' for key 'users.phone'");
        var ex = new DataIntegrityViolationException("could not execute statement", cause);

        ResponseEntity<ApiResponse<?>> response = handler.handleDataIntegrityViolation(ex);

        assertThat(response.getBody().getError().getMessage()).isEqualTo("이미 사용 중인 휴대폰 번호입니다");
    }

    @Test
    void handleDataIntegrityViolation_닉네임_제약조건_구체적_메시지_반환() {
        var cause = new RuntimeException("Duplicate entry '여행자' for key 'users.nickname'");
        var ex = new DataIntegrityViolationException("could not execute statement", cause);

        ResponseEntity<ApiResponse<?>> response = handler.handleDataIntegrityViolation(ex);

        assertThat(response.getBody().getError().getMessage()).isEqualTo("이미 사용 중인 닉네임입니다");
    }

    @Test
    void handleDataIntegrityViolation_알수없는_제약조건_제네릭_메시지_반환() {
        var cause = new RuntimeException("Duplicate entry 'xxx' for key 'other_table.some_column'");
        var ex = new DataIntegrityViolationException("could not execute statement", cause);

        ResponseEntity<ApiResponse<?>> response = handler.handleDataIntegrityViolation(ex);

        assertThat(response.getBody().getError().getMessage()).isEqualTo("데이터 무결성 제약 조건 위반입니다");
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
