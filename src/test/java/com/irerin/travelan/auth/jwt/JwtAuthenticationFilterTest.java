package com.irerin.travelan.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.irerin.travelan.user.entity.UserRole;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtProvider jwtProvider;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtProvider);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 유효한_Bearer_토큰_SecurityContext_인증_설정() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");

        given(jwtProvider.isValid("valid-token")).willReturn(true);
        given(jwtProvider.getUserId("valid-token")).willReturn(1L);
        given(jwtProvider.getRole("valid-token")).willReturn(UserRole.USER);

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(1L);
        assertThat(auth.getAuthorities()).extracting("authority")
            .containsExactly("ROLE_USER");
    }

    @Test
    void Authorization_헤더_없으면_SecurityContext_미설정() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void 유효하지_않은_토큰_SecurityContext_미설정() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");

        given(jwtProvider.isValid("invalid-token")).willReturn(false);

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
