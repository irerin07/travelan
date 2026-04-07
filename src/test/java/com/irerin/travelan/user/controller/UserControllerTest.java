package com.irerin.travelan.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.irerin.travelan.auth.jwt.JwtProvider;
import com.irerin.travelan.common.config.SecurityConfig;
import com.irerin.travelan.common.exception.NotFoundException;
import com.irerin.travelan.user.entity.UserRole;
import com.irerin.travelan.user.service.UserService;

@WebMvcTest(controllers = UserController.class)
@Import(SecurityConfig.class)
@org.springframework.boot.test.autoconfigure.json.AutoConfigureJson
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean UserService userService;
    @MockitoBean JwtProvider jwtProvider;

    @Test
    void withdraw_인증된_사용자_204_반환() throws Exception {
        given(jwtProvider.isValid("valid-token")).willReturn(true);
        given(jwtProvider.getUserId("valid-token")).willReturn(1L);
        given(jwtProvider.getRole("valid-token")).willReturn(UserRole.USER);

        mockMvc.perform(delete("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
            .andExpect(status().isNoContent());
    }

    @Test
    void withdraw_응답에_RefreshToken_Cookie_만료_헤더_포함() throws Exception {
        given(jwtProvider.isValid("valid-token")).willReturn(true);
        given(jwtProvider.getUserId("valid-token")).willReturn(1L);
        given(jwtProvider.getRole("valid-token")).willReturn(UserRole.USER);

        mockMvc.perform(delete("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/v1/auth")));
    }

    @Test
    void withdraw_미인증_401_반환() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void withdraw_이미_탈퇴된_회원_404_반환() throws Exception {
        given(jwtProvider.isValid("valid-token")).willReturn(true);
        given(jwtProvider.getUserId("valid-token")).willReturn(1L);
        given(jwtProvider.getRole("valid-token")).willReturn(UserRole.USER);
        willThrow(new NotFoundException("존재하지 않는 회원입니다"))
            .given(userService).withdraw(1L);

        mockMvc.perform(delete("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    // ── @AuthenticationPrincipal null-safety guarantee tests ─────────────────
    // SecurityConfig 에 .anyRequest().authenticated() 가 설정되어 있으므로,
    // 유효한 Authentication 없이 들어온 요청은 필터 체인에서 401 로 차단된다.
    // JwtAuthenticationFilter 는 UsernamePasswordAuthenticationToken(userId, ...)
    // 로 항상 non-null Long 을 principal 에 채운다.
    // 따라서 @AuthenticationPrincipal Long userId 가 컨트롤러에 도달할 때
    // null 일 수 있는 경로는 존재하지 않으며, 컨트롤러 내부의 null 체크는 불필요하다.

    @Test
    void withdraw_미인증_요청은_필터에서_차단되어_서비스가_호출되지_않는다() throws Exception {
        // Given: Authorization 헤더가 없는 요청 (principal이 null이 될 수 있는 유일한 경로)

        // When
        mockMvc.perform(delete("/api/v1/users/me"))

        // Then: 필터 체인의 authenticationEntryPoint 가 401 을 반환하고,
        //       컨트롤러 메서드 자체는 실행되지 않으므로 서비스도 호출되지 않는다.
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    void withdraw_인증된_요청은_서비스에_non_null_userId를_전달한다() throws Exception {
        // Given: JwtAuthenticationFilter 가 token 을 검증하고 userId=1L 을 principal 로 설정
        given(jwtProvider.isValid("valid-token")).willReturn(true);
        given(jwtProvider.getUserId("valid-token")).willReturn(1L);
        given(jwtProvider.getRole("valid-token")).willReturn(UserRole.USER);

        // When
        mockMvc.perform(delete("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
            .andExpect(status().isNoContent());

        // Then: 서비스에 전달된 userId 가 null 이 아님을 캡처로 검증
        //       이것이 컨트롤러 내부 null 체크가 불필요함을 증명한다.
        ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(userService).withdraw(userIdCaptor.capture());
        assertThat(userIdCaptor.getValue()).isNotNull();
    }
}
