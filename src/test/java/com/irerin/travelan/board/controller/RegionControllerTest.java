package com.irerin.travelan.board.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.irerin.travelan.auth.jwt.JwtProvider;
import com.irerin.travelan.auth.support.AuthCookieFactory;
import com.irerin.travelan.board.dto.RegionResponse;
import com.irerin.travelan.board.entity.Region;
import com.irerin.travelan.board.service.RegionService;
import com.irerin.travelan.common.config.SecurityConfig;
import com.irerin.travelan.user.repository.UserRepository;

@WebMvcTest(controllers = RegionController.class)
@Import({SecurityConfig.class, AuthCookieFactory.class})
@org.springframework.boot.test.autoconfigure.json.AutoConfigureJson
class RegionControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean RegionService regionService;
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean UserRepository userRepository;

    @Test
    void list_allowsAnonymousAndReturnsRegions() throws Exception {
        RegionResponse seoul = RegionResponse.from(Region.of("seoul", "서울", "서울특별시", 1, true));
        RegionResponse jeju = RegionResponse.from(Region.of("jeju", "제주", "제주특별자치도", 8, true));
        given(regionService.findAllActive()).willReturn(List.of(seoul, jeju));

        mockMvc.perform(get("/api/v1/regions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].code").value("seoul"))
            .andExpect(jsonPath("$.data[0].name").value("서울"))
            .andExpect(jsonPath("$.data[1].code").value("jeju"));
    }
}
