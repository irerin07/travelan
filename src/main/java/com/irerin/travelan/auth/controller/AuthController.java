package com.irerin.travelan.auth.controller;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.irerin.travelan.common.exception.AuthException;
import com.irerin.travelan.auth.dto.AvailableResponse;
import com.irerin.travelan.auth.dto.LoginCommand;
import com.irerin.travelan.auth.dto.LoginRequest;
import com.irerin.travelan.auth.dto.LoginResponse;
import com.irerin.travelan.auth.dto.LoginTokens;
import com.irerin.travelan.auth.dto.SignupRequest;
import com.irerin.travelan.auth.dto.SignupResponse;
import com.irerin.travelan.auth.jwt.JwtProperties;
import com.irerin.travelan.auth.jwt.JwtProvider;
import com.irerin.travelan.auth.service.AuthService;
import com.irerin.travelan.common.response.ApiResponse;
import com.irerin.travelan.user.dto.SignupCommand;
import com.irerin.travelan.user.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final JwtProperties jwtProperties;
    private final JwtProvider jwtProvider;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@RequestBody @Valid SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(userService.signup(SignupCommand.from(request))));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
        @RequestBody @Valid LoginRequest request,
        HttpServletResponse httpResponse
    ) {
        LoginTokens tokens = authService.login(LoginCommand.from(request));

        ResponseCookie cookie = ResponseCookie.from("refreshToken", tokens.getRefreshToken())
            .httpOnly(true)
            .secure(true)
            .sameSite("Strict")
            .maxAge(Duration.ofSeconds(jwtProperties.getRefreshTokenExpiry()))
            .path("/api/v1/auth")
            .build();

        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(ApiResponse.ok(
            LoginResponse.of(tokens.getAccessToken(), "Bearer", tokens.getAccessTokenExpiry())
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
        @CookieValue(name = "refreshToken", required = false) String refreshToken,
        HttpServletResponse httpResponse
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthException("리프레시 토큰이 없습니다");
        }

        LoginTokens tokens = authService.refresh(refreshToken);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", tokens.getRefreshToken())
            .httpOnly(true)
            .secure(true)
            .sameSite("Strict")
            .maxAge(Duration.ofSeconds(jwtProperties.getRefreshTokenExpiry()))
            .path("/api/v1/auth")
            .build();

        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(ApiResponse.ok(
            LoginResponse.of(tokens.getAccessToken(), "Bearer", tokens.getAccessTokenExpiry())
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse httpResponse) {
        String bearer = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (bearer == null || !bearer.startsWith("Bearer ")) {
            throw new AuthException("인증 토큰이 필요합니다");
        }

        String token = bearer.substring(7);
        if (!jwtProvider.isValid(token)) {
            throw new AuthException("유효하지 않은 토큰입니다");
        }

        Long userId = jwtProvider.getUserId(token);
        authService.logout(userId);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
            .httpOnly(true)
            .secure(true)
            .sameSite("Strict")
            .maxAge(0)
            .path("/api/v1/auth")
            .build();

        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<AvailableResponse>> checkEmail(
        @RequestParam @NotBlank(message = "이메일은 필수입니다") @Email(message = "이메일 형식이 올바르지 않습니다") String email
    ) {
        return ResponseEntity.ok(ApiResponse.ok(AvailableResponse.of(userService.isEmailAvailable(email))));
    }

    @GetMapping("/check-phone")
    public ResponseEntity<ApiResponse<AvailableResponse>> checkPhone(
        @RequestParam @NotBlank(message = "휴대폰 번호는 필수입니다") @Pattern(regexp = "^010\\d{8}$", message = "휴대폰 번호 형식이 올바르지 않습니다") String phone
    ) {
        return ResponseEntity.ok(ApiResponse.ok(AvailableResponse.of(userService.isPhoneAvailable(phone))));
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<AvailableResponse>> checkNickname(
        @RequestParam @NotBlank(message = "닉네임은 필수입니다") @Size(min = 2, max = 10, message = "닉네임은 2~10자여야 합니다") String nickname
    ) {
        return ResponseEntity.ok(ApiResponse.ok(AvailableResponse.of(userService.isNicknameAvailable(nickname))));
    }

}
