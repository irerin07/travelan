package com.irerin.travelan.board.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.irerin.travelan.auth.jwt.JwtProvider;
import com.irerin.travelan.auth.support.AuthCookieFactory;
import com.irerin.travelan.board.dto.ReportPostCommand;
import com.irerin.travelan.board.dto.ReportResponse;
import com.irerin.travelan.board.entity.Post;
import com.irerin.travelan.board.entity.PostReport;
import com.irerin.travelan.board.entity.Region;
import com.irerin.travelan.board.entity.ReportReason;
import com.irerin.travelan.board.service.PostReportService;
import com.irerin.travelan.common.config.SecurityConfig;
import com.irerin.travelan.common.exception.DuplicateException;
import com.irerin.travelan.common.exception.ForbiddenException;
import com.irerin.travelan.user.entity.User;
import com.irerin.travelan.user.entity.UserRole;
import com.irerin.travelan.user.repository.UserRepository;

@WebMvcTest(controllers = PostReportController.class)
@Import({SecurityConfig.class, AuthCookieFactory.class})
@org.springframework.boot.test.autoconfigure.json.AutoConfigureJson
class PostReportControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean PostReportService postReportService;
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean UserRepository userRepository;

    private static final String TOKEN = "valid-token";

    @BeforeEach
    void setUp() {
        given(jwtProvider.isValid(TOKEN)).willReturn(true);
        given(jwtProvider.getUserId(TOKEN)).willReturn(2L);
        given(jwtProvider.getRole(TOKEN)).willReturn(UserRole.USER);
        User active = User.of("b@x.com", "p", "신고자", "01011111111", "신고자닉");
        ReflectionTestUtils.setField(active, "id", 2L);
        given(userRepository.findById(2L)).willReturn(Optional.of(active));
    }

    @Test
    void report_requiresAuth_401WithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/posts/100/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"SPAM\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void report_withValidToken_returns201() throws Exception {
        Region region = Region.of("seoul", "서울", "desc", 1, true);
        User author = User.of("a@x.com", "p", "작성자", "01000000000", "작성자닉");
        User reporterUser = User.of("b@x.com", "p", "신고자", "01011111111", "신고자닉");
        Post post = Post.of(region, author, "제목", "내용");
        PostReport postReport = PostReport.of(post, reporterUser, ReportReason.SPAM);
        ReflectionTestUtils.setField(postReport, "id", 999L);
        ReflectionTestUtils.setField(postReport, "createdAt", LocalDateTime.now());
        ReportResponse response = ReportResponse.from(postReport);
        given(postReportService.report(any(ReportPostCommand.class))).willReturn(response);

        mockMvc.perform(post("/api/v1/posts/100/reports")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"SPAM\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.reportId").value(999));
    }

    @Test
    void report_selfReport_returns403() throws Exception {
        willThrow(new ForbiddenException("자신의 게시글은 신고할 수 없습니다"))
            .given(postReportService).report(any(ReportPostCommand.class));

        mockMvc.perform(post("/api/v1/posts/100/reports")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"SPAM\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void report_duplicate_returns409() throws Exception {
        willThrow(new DuplicateException("이미 신고한 게시글입니다"))
            .given(postReportService).report(any(ReportPostCommand.class));

        mockMvc.perform(post("/api/v1/posts/100/reports")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"SPAM\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("DUPLICATE"));
    }

    @Test
    void report_missingReason_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/posts/100/reports")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }
}
