package com.irerin.travelan.auth.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

class AuthCookieFactoryTest {

    private AuthCookieFactory authCookieFactory;

    @BeforeEach
    void setUp() {
        authCookieFactory = new AuthCookieFactory();
    }

    @Test
    void refreshTokenCookie_이름이_refreshToken이다() {
        ResponseCookie cookie = authCookieFactory.refreshTokenCookie("some-value", Duration.ofSeconds(3600));
        assertThat(cookie.getName()).isEqualTo("refreshToken");
    }

    @Test
    void refreshTokenCookie_값이_설정된다() {
        ResponseCookie cookie = authCookieFactory.refreshTokenCookie("token-abc", Duration.ofSeconds(3600));
        assertThat(cookie.getValue()).isEqualTo("token-abc");
    }

    @Test
    void refreshTokenCookie_maxAge가_설정된다() {
        ResponseCookie cookie = authCookieFactory.refreshTokenCookie("v", Duration.ofSeconds(7200));
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofSeconds(7200));
    }

    @Test
    void refreshTokenCookie_httpOnly_true() {
        ResponseCookie cookie = authCookieFactory.refreshTokenCookie("v", Duration.ofSeconds(3600));
        assertThat(cookie.isHttpOnly()).isTrue();
    }

    @Test
    void refreshTokenCookie_secure_true() {
        ResponseCookie cookie = authCookieFactory.refreshTokenCookie("v", Duration.ofSeconds(3600));
        assertThat(cookie.isSecure()).isTrue();
    }

    @Test
    void refreshTokenCookie_sameSite_Strict() {
        ResponseCookie cookie = authCookieFactory.refreshTokenCookie("v", Duration.ofSeconds(3600));
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
    }

    @Test
    void refreshTokenCookie_path가_REFRESH_TOKEN_PATH_상수와_일치한다() {
        ResponseCookie cookie = authCookieFactory.refreshTokenCookie("v", Duration.ofSeconds(3600));
        assertThat(cookie.getPath()).isEqualTo(AuthCookieFactory.REFRESH_TOKEN_PATH);
    }

    @Test
    void expiredRefreshTokenCookie_이름이_refreshToken이다() {
        ResponseCookie cookie = authCookieFactory.expiredRefreshTokenCookie();
        assertThat(cookie.getName()).isEqualTo("refreshToken");
    }

    @Test
    void expiredRefreshTokenCookie_값이_빈문자열이다() {
        ResponseCookie cookie = authCookieFactory.expiredRefreshTokenCookie();
        assertThat(cookie.getValue()).isEmpty();
    }

    @Test
    void expiredRefreshTokenCookie_maxAge가_0이다() {
        ResponseCookie cookie = authCookieFactory.expiredRefreshTokenCookie();
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
    }

    @Test
    void expiredRefreshTokenCookie_httpOnly_true() {
        ResponseCookie cookie = authCookieFactory.expiredRefreshTokenCookie();
        assertThat(cookie.isHttpOnly()).isTrue();
    }

    @Test
    void expiredRefreshTokenCookie_secure_true() {
        ResponseCookie cookie = authCookieFactory.expiredRefreshTokenCookie();
        assertThat(cookie.isSecure()).isTrue();
    }

    @Test
    void expiredRefreshTokenCookie_sameSite_Strict() {
        ResponseCookie cookie = authCookieFactory.expiredRefreshTokenCookie();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
    }

    @Test
    void expiredRefreshTokenCookie_path가_REFRESH_TOKEN_PATH_상수와_일치한다() {
        ResponseCookie cookie = authCookieFactory.expiredRefreshTokenCookie();
        assertThat(cookie.getPath()).isEqualTo(AuthCookieFactory.REFRESH_TOKEN_PATH);
    }

    @Test
    void REFRESH_TOKEN_PATH_상수가_api_v1_auth_경로이다() {
        assertThat(AuthCookieFactory.REFRESH_TOKEN_PATH).isEqualTo("/api/v1/auth");
    }
}
