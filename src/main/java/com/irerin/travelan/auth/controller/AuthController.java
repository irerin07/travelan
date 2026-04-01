package com.irerin.travelan.auth.controller;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.irerin.travelan.auth.dto.AvailableResponse;
import com.irerin.travelan.auth.dto.LoginCommand;
import com.irerin.travelan.auth.dto.LoginRequest;
import com.irerin.travelan.auth.dto.LoginResponse;
import com.irerin.travelan.auth.dto.LoginTokens;
import com.irerin.travelan.auth.dto.SignupRequest;
import com.irerin.travelan.auth.dto.SignupResponse;
import com.irerin.travelan.auth.service.AuthService;
import com.irerin.travelan.common.response.ApiResponse;
import com.irerin.travelan.user.dto.SignupCommand;
import com.irerin.travelan.user.service.UserService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

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
            .maxAge(Duration.ofDays(30))
            .path("/api/v1/auth")
            .build();
        
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(ApiResponse.ok(
            LoginResponse.of(tokens.getAccessToken(), "Bearer", tokens.getAccessTokenExpiry())
        ));
    }

    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<AvailableResponse>> checkEmail(@RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.ok(AvailableResponse.of(userService.isEmailAvailable(email))));
    }

    @GetMapping("/check-phone")
    public ResponseEntity<ApiResponse<AvailableResponse>> checkPhone(@RequestParam String phone) {
        return ResponseEntity.ok(ApiResponse.ok(AvailableResponse.of(userService.isPhoneAvailable(phone))));
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<AvailableResponse>> checkNickname(@RequestParam String nickname) {
        return ResponseEntity.ok(ApiResponse.ok(AvailableResponse.of(userService.isNicknameAvailable(nickname))));
    }

}
