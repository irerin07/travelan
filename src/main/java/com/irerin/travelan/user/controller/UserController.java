package com.irerin.travelan.user.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.irerin.travelan.auth.support.AuthCookieFactory;
import com.irerin.travelan.common.response.ApiResponse;
import com.irerin.travelan.user.dto.MyPageResponse;
import com.irerin.travelan.user.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthCookieFactory authCookieFactory;

    @GetMapping("/me")
    public ApiResponse<MyPageResponse> getMyPage(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(MyPageResponse.from(userService.findById(userId)));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(
        @AuthenticationPrincipal Long userId
    ) {
        userService.withdraw(userId);

        ResponseCookie cookie = authCookieFactory.expiredRefreshTokenCookie();

        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .build();
    }
}
