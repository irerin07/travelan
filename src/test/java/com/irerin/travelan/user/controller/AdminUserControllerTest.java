package com.irerin.travelan.user.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.irerin.travelan.admin.dto.UserSummaryResponse;
import com.irerin.travelan.auth.jwt.JwtProvider;
import com.irerin.travelan.common.config.SecurityConfig;
import com.irerin.travelan.user.entity.User;
import com.irerin.travelan.user.entity.UserRole;
import com.irerin.travelan.user.entity.UserStatus;
import com.irerin.travelan.user.service.UserService;

@WebMvcTest(controllers = AdminUserController.class)
@Import(SecurityConfig.class)
@org.springframework.boot.test.autoconfigure.json.AutoConfigureJson
class AdminUserControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean UserService userService;
    @MockitoBean JwtProvider jwtProvider;

    private Page<UserSummaryResponse> pagedResult;

    @BeforeEach
    void setUp() {
        User user1 = User.builder()
            .email("user@example.com").password("encoded").name("홍길동")
            .phone("01012345678").nickname("여행자").build();
        ReflectionTestUtils.setField(user1, "id", 1L);
        ReflectionTestUtils.setField(user1, "role", UserRole.USER);
        ReflectionTestUtils.setField(user1, "status", UserStatus.ACTIVE);
        ReflectionTestUtils.setField(user1, "createdAt", LocalDateTime.of(2026, 3, 31, 10, 0));

        User user2 = User.builder()
            .email("admin@example.com").password("encoded").name("관리자")
            .phone("01099999999").nickname("관리자닉").build();
        ReflectionTestUtils.setField(user2, "id", 2L);
        ReflectionTestUtils.setField(user2, "role", UserRole.ADMIN);
        ReflectionTestUtils.setField(user2, "status", UserStatus.ACTIVE);
        ReflectionTestUtils.setField(user2, "createdAt", LocalDateTime.of(2026, 3, 31, 11, 0));

        List<UserSummaryResponse> content = List.of(
            UserSummaryResponse.from(user1), UserSummaryResponse.from(user2));
        pagedResult = new PageImpl<>(content, PageRequest.of(0, 20), 2);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listUsers_ADMIN_200반환() throws Exception {
        given(userService.findUsers(eq(1), eq(20))).willReturn(pagedResult);

        mockMvc.perform(get("/api/v1/admin/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].id").value(1))
            .andExpect(jsonPath("$.data[0].email").value("user@example.com"))
            .andExpect(jsonPath("$.data[0].role").value("USER"))
            .andExpect(jsonPath("$.data[1].role").value("ADMIN"))
            .andExpect(jsonPath("$.page.page").value(1))
            .andExpect(jsonPath("$.page.size").value(20))
            .andExpect(jsonPath("$.page.totalElements").value(2))
            .andExpect(jsonPath("$.page.totalPages").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listUsers_페이지_파라미터_적용() throws Exception {
        Page<UserSummaryResponse> page2Result =
            new PageImpl<>(List.of(), PageRequest.of(1, 5), 2);
        given(userService.findUsers(eq(2), eq(5))).willReturn(page2Result);

        mockMvc.perform(get("/api/v1/admin/users").param("page", "2").param("size", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.page").value(2))
            .andExpect(jsonPath("$.page.size").value(5))
            .andExpect(jsonPath("$.page.totalElements").value(2))
            .andExpect(jsonPath("$.page.totalPages").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listUsers_빈페이지이면_빈배열반환() throws Exception {
        given(userService.findUsers(eq(1), eq(20)))
            .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/admin/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data").isEmpty())
            .andExpect(jsonPath("$.page.totalElements").value(0))
            .andExpect(jsonPath("$.page.totalPages").value(0));
    }

    @Test
    @WithMockUser(roles = "USER")
    void listUsers_USER역할이면_403반환() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void listUsers_미인증이면_401반환() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
}
