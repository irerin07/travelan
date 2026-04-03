package com.irerin.travelan.auth.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

import com.irerin.travelan.auth.dto.LoginCommand;
import com.irerin.travelan.auth.dto.LoginTokens;
import com.irerin.travelan.auth.dto.SignupResponse;
import com.irerin.travelan.auth.jwt.JwtProvider;
import com.irerin.travelan.auth.service.AuthService;
import com.irerin.travelan.common.config.SecurityConfig;
import com.irerin.travelan.common.exception.AuthException;
import com.irerin.travelan.common.exception.DuplicateException;
import com.irerin.travelan.user.dto.SignupCommand;
import com.irerin.travelan.user.entity.User;
import com.irerin.travelan.user.service.UserService;

import org.springframework.http.HttpHeaders;

@WebMvcTest(controllers = AuthController.class)
@Import(SecurityConfig.class)
@org.springframework.boot.test.autoconfigure.json.AutoConfigureJson
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean UserService userService;
    @MockitoBean AuthService authService;
    @MockitoBean JwtProvider jwtProvider;

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
        signupResponse = SignupResponse.from(user);
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

    // ── check-* validation ─────────────────────────────────────────────────

    @Test
    void checkEmail_빈값이면_400() throws Exception {
        mockMvc.perform(get("/api/v1/auth/check-email").param("email", ""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void checkEmail_형식오류이면_400() throws Exception {
        mockMvc.perform(get("/api/v1/auth/check-email").param("email", "invalid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void checkPhone_형식오류이면_400() throws Exception {
        mockMvc.perform(get("/api/v1/auth/check-phone").param("phone", "12345"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void checkNickname_길이미달이면_400() throws Exception {
        mockMvc.perform(get("/api/v1/auth/check-nickname").param("nickname", "a"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void checkEmail_형식오류시_field가_email() throws Exception {
        mockMvc.perform(get("/api/v1/auth/check-email").param("email", "invalid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.errors[0].field").value("email"));
    }

    @Test
    void checkPhone_형식오류시_field가_phone() throws Exception {
        mockMvc.perform(get("/api/v1/auth/check-phone").param("phone", "12345"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.errors[0].field").value("phone"));
    }

    @Test
    void checkNickname_길이미달시_field가_nickname() throws Exception {
        mockMvc.perform(get("/api/v1/auth/check-nickname").param("nickname", "a"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.errors[0].field").value("nickname"));
    }

    // ── POST /api/v1/auth/login ──────────────────────────────────────────────

    @Test
    void login_성공_200_accessToken_반환() throws Exception {
        given(authService.login(any(LoginCommand.class)))
            .willReturn(LoginTokens.of("access-token", "refresh-token", 3600L));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "email": "test@example.com", "password": "Password1!" }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").value("access-token"))
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.data.expiresIn").value(3600))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=refresh-token")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")));
    }

    @Test
    void login_Set_Cookie_Secure_SameSite_Path_검증() throws Exception {
        given(authService.login(any(LoginCommand.class)))
            .willReturn(LoginTokens.of("access-token", "refresh-token", 3600L));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "email": "test@example.com", "password": "Password1!" }
                    """))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Secure")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Strict")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/v1/auth")));
    }

    @Test
    void login_잘못된_자격증명_401반환() throws Exception {
        willThrow(new AuthException("이메일 또는 비밀번호가 올바르지 않습니다"))
            .given(authService).login(any(LoginCommand.class));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "email": "test@example.com", "password": "wrongPassword" }
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void login_이메일_빈값_400반환() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "email": "", "password": "Password1!" }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    // ── POST /api/v1/auth/refresh ───────────────────────────────────────

    @Test
    void refresh_성공_새_accessToken_반환_및_Cookie_갱신() throws Exception {
        given(authService.refresh("old-refresh-token"))
            .willReturn(LoginTokens.of("new-access-token", "new-refresh-token", 3600L));

        mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(new jakarta.servlet.http.Cookie("refreshToken", "old-refresh-token")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.data.expiresIn").value(3600))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=new-refresh-token")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")));
    }

    @Test
    void refresh_쿠키_없으면_401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void refresh_Cookie_Secure_SameSite_Path_검증() throws Exception {
        given(authService.refresh("old-refresh-token"))
            .willReturn(LoginTokens.of("new-access-token", "new-refresh-token", 3600L));

        mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(new jakarta.servlet.http.Cookie("refreshToken", "old-refresh-token")))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Secure")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Strict")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/v1/auth")));
    }

    @Test
    void refresh_토큰_재사용_감지_401() throws Exception {
        willThrow(new AuthException("토큰 재사용이 감지되었습니다"))
            .given(authService).refresh("revoked-token");

        mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(new jakarta.servlet.http.Cookie("refreshToken", "revoked-token")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    // ── POST /api/v1/auth/logout ────────────────────────────────────────

    @Test
    void logout_성공_204_Cookie_삭제() throws Exception {
        given(jwtProvider.isValid("valid-access-token")).willReturn(true);
        given(jwtProvider.getUserId("valid-access-token")).willReturn(1L);
        given(jwtProvider.getRole("valid-access-token")).willReturn(com.irerin.travelan.user.entity.UserRole.USER);

        mockMvc.perform(post("/api/v1/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-access-token"))
            .andExpect(status().isNoContent())
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
    }

    @Test
    void logout_토큰없이_요청_401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void logout_유효하지않은_토큰_401() throws Exception {
        given(jwtProvider.isValid("invalid-token")).willReturn(false);

        mockMvc.perform(post("/api/v1/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
}
