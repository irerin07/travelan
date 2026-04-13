package com.irerin.travelan.board.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.irerin.travelan.auth.jwt.JwtProvider;
import com.irerin.travelan.auth.support.AuthCookieFactory;
import com.irerin.travelan.board.dto.PostImageResponse;
import com.irerin.travelan.board.dto.UploadImageCommand;
import com.irerin.travelan.board.entity.PostImage;
import com.irerin.travelan.board.service.ImageUploadService;
import com.irerin.travelan.common.config.SecurityConfig;
import com.irerin.travelan.user.entity.User;
import com.irerin.travelan.user.entity.UserRole;
import com.irerin.travelan.user.repository.UserRepository;

@WebMvcTest(controllers = ImageController.class)
@Import({SecurityConfig.class, AuthCookieFactory.class})
class ImageControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ImageUploadService imageUploadService;
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean UserRepository userRepository;

    private static final String TOKEN = "valid-token";

    @BeforeEach
    void setUp() {
        given(jwtProvider.isValid(TOKEN)).willReturn(true);
        given(jwtProvider.getUserId(TOKEN)).willReturn(1L);
        given(jwtProvider.getRole(TOKEN)).willReturn(UserRole.USER);
        User user = User.of("a@x.com", "p", "홍길동", "01000000000", "여행자");
        ReflectionTestUtils.setField(user, "id", 1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
    }

    @Test
    void upload_returns201WithImageResponses() throws Exception {
        PostImage image = PostImage.of("/uploads/posts/1/2026/04/uuid.jpg", "photo.jpg", 1024);
        ReflectionTestUtils.setField(image, "id", 1L);
        given(imageUploadService.upload(any(UploadImageCommand.class)))
            .willReturn(List.of(PostImageResponse.from(image)));

        MockMultipartFile file = new MockMultipartFile(
            "images", "photo.jpg", "image/jpeg", "fake".getBytes());

        mockMvc.perform(multipart("/api/v1/posts/images")
                .file(file)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data[0].id").value(1))
            .andExpect(jsonPath("$.data[0].url").exists());
    }

    @Test
    void upload_returns401WithoutToken() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "images", "photo.jpg", "image/jpeg", "fake".getBytes());

        mockMvc.perform(multipart("/api/v1/posts/images").file(file))
            .andExpect(status().isUnauthorized());
    }
}
