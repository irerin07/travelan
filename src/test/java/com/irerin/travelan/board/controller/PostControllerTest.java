package com.irerin.travelan.board.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.irerin.travelan.auth.jwt.JwtProvider;
import com.irerin.travelan.auth.support.AuthCookieFactory;
import com.irerin.travelan.board.dto.CreatePostCommand;
import com.irerin.travelan.board.dto.PostDetailResponse;
import com.irerin.travelan.board.dto.PostSummaryResponse;
import com.irerin.travelan.board.dto.UpdatePostCommand;
import com.irerin.travelan.board.entity.Post;
import com.irerin.travelan.board.entity.Region;
import com.irerin.travelan.board.service.PostService;
import com.irerin.travelan.common.config.SecurityConfig;
import com.irerin.travelan.common.exception.ForbiddenException;
import com.irerin.travelan.user.entity.User;
import com.irerin.travelan.user.entity.UserRole;
import com.irerin.travelan.user.repository.UserRepository;

@WebMvcTest(controllers = PostController.class)
@Import({SecurityConfig.class, AuthCookieFactory.class})
@org.springframework.boot.test.autoconfigure.json.AutoConfigureJson
class PostControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean PostService postService;
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean UserRepository userRepository;

    private static final String TOKEN = "valid-token";
    private PostDetailResponse detail;

    @BeforeEach
    void setUp() {
        Region region = Region.of("seoul", "서울", "desc", 1, true);
        ReflectionTestUtils.setField(region, "id", 10L);
        User author = User.of("a@x.com", "p", "홍길동", "01000000000", "여행자");
        ReflectionTestUtils.setField(author, "id", 1L);
        Post post = Post.of(region, author, "title", "<p>content</p>");
        ReflectionTestUtils.setField(post, "id", 100L);
        ReflectionTestUtils.setField(post, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(post, "updatedAt", LocalDateTime.now());
        detail = PostDetailResponse.from(post, List.of());

        given(jwtProvider.isValid(TOKEN)).willReturn(true);
        given(jwtProvider.getUserId(TOKEN)).willReturn(1L);
        given(jwtProvider.getRole(TOKEN)).willReturn(UserRole.USER);
        User active = User.of("a@x.com", "p", "홍길동", "01000000000", "여행자");
        ReflectionTestUtils.setField(active, "id", 1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(active));
    }

    @Test
    void list_anonymousAllowed() throws Exception {
        Region region = Region.of("seoul", "서울", "desc", 1, true);
        ReflectionTestUtils.setField(region, "id", 10L);
        User author = User.of("a@x.com", "p", "홍길동", "01000000000", "여행자");
        ReflectionTestUtils.setField(author, "id", 1L);
        Post post = Post.of(region, author, "title", "c");
        ReflectionTestUtils.setField(post, "id", 100L);
        ReflectionTestUtils.setField(post, "createdAt", LocalDateTime.now());
        PostSummaryResponse summary = PostSummaryResponse.from(post);
        given(postService.listByRegion(eqStr("seoul"), eq(1), eq(20)))
            .willReturn(new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/regions/seoul/posts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].id").value(100));
    }

    @Test
    void listAll_anonymousAllowed() throws Exception {
        Region region = Region.of("seoul", "서울", "desc", 1, true);
        ReflectionTestUtils.setField(region, "id", 10L);
        User author = User.of("a@x.com", "p", "홍길동", "01000000000", "여행자");
        ReflectionTestUtils.setField(author, "id", 1L);
        Post post = Post.of(region, author, "title", "c");
        ReflectionTestUtils.setField(post, "id", 200L);
        ReflectionTestUtils.setField(post, "createdAt", LocalDateTime.now());
        PostSummaryResponse summary = PostSummaryResponse.from(post);
        given(postService.listAll(eq(1), eq(20)))
            .willReturn(new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/posts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].id").value(200));
    }

    @Test
    void listAll_returns400_whenPageIsZero() throws Exception {
        mockMvc.perform(get("/api/v1/posts").param("page", "0"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void listAll_returns400_whenSizeExceeds100() throws Exception {
        mockMvc.perform(get("/api/v1/posts").param("size", "101"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void list_returns400_whenRegionCodeContainsUppercase() throws Exception {
        mockMvc.perform(get("/api/v1/regions/Seoul/posts"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void list_returns400_whenRegionCodeContainsHyphen() throws Exception {
        mockMvc.perform(get("/api/v1/regions/seoul-1/posts"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void list_returns400_whenRegionCodeStartsWithDigit() throws Exception {
        mockMvc.perform(get("/api/v1/regions/1seoul/posts"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void list_returns400_whenRegionCodeContainsSpecialChars() throws Exception {
        mockMvc.perform(get("/api/v1/regions/seoul!/posts"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void list_acceptsRegionCodeWithUnderscoreAndDigits() throws Exception {
        Region region = Region.of("overseas_etc", "기타", "desc", 1, true);
        ReflectionTestUtils.setField(region, "id", 10L);
        User author = User.of("a@x.com", "p", "홍길동", "01000000000", "여행자");
        ReflectionTestUtils.setField(author, "id", 1L);
        Post post = Post.of(region, author, "title", "c");
        ReflectionTestUtils.setField(post, "id", 100L);
        ReflectionTestUtils.setField(post, "createdAt", LocalDateTime.now());
        PostSummaryResponse summary = PostSummaryResponse.from(post);
        given(postService.listByRegion(eqStr("overseas_etc"), eq(1), eq(20)))
            .willReturn(new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/regions/overseas_etc/posts"))
            .andExpect(status().isOk());
    }

    private static String eqStr(String s) { return org.mockito.ArgumentMatchers.eq(s); }

    @Test
    void get_anonymousAllowed() throws Exception {
        given(postService.get(100L)).willReturn(detail);

        mockMvc.perform(get("/api/v1/posts/100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(100))
            .andExpect(jsonPath("$.data.title").value("title"))
            .andExpect(jsonPath("$.data.authorId").value(1));
    }

    @Test
    void create_requiresAuth_401WithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/regions/seoul/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"t\",\"content\":\"c\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void create_withTokenReturns201() throws Exception {
        given(postService.create(any(CreatePostCommand.class))).willReturn(detail);

        mockMvc.perform(post("/api/v1/regions/seoul/posts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"title\",\"content\":\"<p>content</p>\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.id").value(100));
    }

    @Test
    void update_forbiddenWhenNotAuthor_403() throws Exception {
        willThrow(new ForbiddenException("게시글 수정 권한이 없습니다"))
            .given(postService).update(any(UpdatePostCommand.class));

        mockMvc.perform(put("/api/v1/posts/100")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"t\",\"content\":\"c\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/posts/100")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
            .andExpect(status().isNoContent());
    }
}
