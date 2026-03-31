package com.irerin.travelan.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.irerin.travelan.auth.dto.AvailableResponse;
import com.irerin.travelan.auth.dto.SignupRequest;
import com.irerin.travelan.auth.dto.SignupResponse;
import com.irerin.travelan.user.dto.SignupCommand;
import com.irerin.travelan.common.response.ApiResponse;
import com.irerin.travelan.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@RequestBody @Valid SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(userService.signup(SignupCommand.from(request))));
    }

    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<AvailableResponse>> checkEmail(@RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.ok(new AvailableResponse(userService.isEmailAvailable(email))));
    }

    @GetMapping("/check-phone")
    public ResponseEntity<ApiResponse<AvailableResponse>> checkPhone(@RequestParam String phone) {
        return ResponseEntity.ok(ApiResponse.ok(new AvailableResponse(userService.isPhoneAvailable(phone))));
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<AvailableResponse>> checkNickname(@RequestParam String nickname) {
        return ResponseEntity.ok(ApiResponse.ok(new AvailableResponse(userService.isNicknameAvailable(nickname))));
    }
}
