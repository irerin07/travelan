package com.irerin.travelan.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.irerin.travelan.auth.dto.SignupResponse;
import com.irerin.travelan.common.config.SecurityConfig;
import com.irerin.travelan.common.exception.DuplicateException;
import com.irerin.travelan.user.dto.SignupCommand;
import com.irerin.travelan.user.entity.User;
import com.irerin.travelan.user.service.UserService;

@WebMvcTest(controllers = AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean UserService userService;

    private SignupResponse signupResponse;

    @BeforeEach
    void setUp() {
        User user = User.builder()
            .email("test@example.com")
            .password("encoded")
            .name("홍길동")
            .phone("01012345678")
            .nickname("여행자")
            .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        signupResponse = new SignupResponse(user);
    }

    private String validSignupJson() {
        return """
            {
              "email": "test@example.com",
              "password": "Password1!",
              "passwordConfirm": "Password1!",
              "name": "홍길동",
              "phone": "01012345678",
              "nickname": "여행자",
              "interestRegions": ["서울", "부산"]
            }
            """;
    }

    // ── POST /api/v1/auth/signup ────────────────────────────────────────────

    @Test
    void signup_정상요청_201반환() throws Exception {
        given(userService.signup(any(SignupCommand.class))).willReturn(signupResponse);

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validSignupJson()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.email").value("test@example.com"))
            .andExpect(jsonPath("$.data.nickname").value("여행자"));
    }

    @Test
    void signup_이메일_공백이면_400() throws Exception {
        String body = validSignupJson().replace("\"test@example.com\"", "\"\"");

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void signup_이메일_형식오류_400() throws Exception {
        String body = validSignupJson().replace("\"test@example.com\"", "\"not-an-email\"");

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void signup_비밀번호_7자이하이면_400() throws Exception {
        String body = validSignupJson()
            .replace("\"Password1!\"", "\"Pass1!\"")
            .replace("\"passwordConfirm\": \"Password1!\"", "\"passwordConfirm\": \"Pass1!\"");

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void signup_비밀번호에_공백포함이면_400() throws Exception {
        String body = validSignupJson()
            .replace("\"Password1!\"", "\"Pass word1!\"")
            .replace("\"passwordConfirm\": \"Password1!\"", "\"passwordConfirm\": \"Pass word1!\"");

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void signup_비밀번호_불일치이면_400() throws Exception {
        String body = validSignupJson().replace("\"passwordConfirm\": \"Password1!\"", "\"passwordConfirm\": \"Different1!\"");

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.error.errors[0].field").value("passwordConfirm"));
    }

    @Test
    void signup_이름_공백이면_400() throws Exception {
        String body = validSignupJson().replace("\"홍길동\"", "\"\"");

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void signup_핸드폰번호_형식오류_400() throws Exception {
        String body = validSignupJson().replace("\"01012345678\"", "\"0201234567\"");

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void signup_닉네임_11자이상이면_400() throws Exception {
        String body = validSignupJson().replace("\"여행자\"", "\"닉네임이열한글자입니다\""); // 11글자

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void signup_닉네임에_공백포함이면_400() throws Exception {
        String body = validSignupJson().replace("\"여행자\"", "\"여행 자\"");

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void signup_관심지역_6개이상이면_400() throws Exception {
        String body = validSignupJson().replace(
            "[\"서울\", \"부산\"]",
            "[\"서울\", \"부산\", \"제주\", \"강릉\", \"전주\", \"경주\"]"
        );

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void signup_이메일_중복이면_409() throws Exception {
        willThrow(new DuplicateException("이미 사용 중인 이메일입니다"))
            .given(userService).signup(any(SignupCommand.class));

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validSignupJson()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("DUPLICATE"))
            .andExpect(jsonPath("$.error.message").value("이미 사용 중인 이메일입니다"));
    }

    // ── GET /api/v1/auth/check-email ────────────────────────────────────────

    @Test
    void checkEmail_사용가능이면_available_true() throws Exception {
        given(userService.isEmailAvailable("new@example.com")).willReturn(true);

        mockMvc.perform(get("/api/v1/auth/check-email").param("email", "new@example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    void checkEmail_이미사용중이면_available_false() throws Exception {
        given(userService.isEmailAvailable("used@example.com")).willReturn(false);

        mockMvc.perform(get("/api/v1/auth/check-email").param("email", "used@example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.available").value(false));
    }

    // ── GET /api/v1/auth/check-phone ────────────────────────────────────────

    @Test
    void checkPhone_사용가능이면_available_true() throws Exception {
        given(userService.isPhoneAvailable("01099999999")).willReturn(true);

        mockMvc.perform(get("/api/v1/auth/check-phone").param("phone", "01099999999"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.available").value(true));
    }

    // ── GET /api/v1/auth/check-nickname ─────────────────────────────────────

    @Test
    void checkNickname_사용가능이면_available_true() throws Exception {
        given(userService.isNicknameAvailable("새닉")).willReturn(true);

        mockMvc.perform(get("/api/v1/auth/check-nickname").param("nickname", "새닉"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.available").value(true));
    }
}
