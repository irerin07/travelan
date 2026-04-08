package com.irerin.travelan.board.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.irerin.travelan.board.dto.RegionResponse;
import com.irerin.travelan.board.entity.Region;
import com.irerin.travelan.board.repository.RegionRepository;

@ExtendWith(MockitoExtension.class)
class RegionServiceTest {

    @Mock RegionRepository regionRepository;
    @InjectMocks RegionService regionService;

    @Test
    void findAllActive_returnsRegionsInDisplayOrder() {
        Region seoul = Region.of("seoul", "서울", "서울특별시", 1, true);
        Region jeju = Region.of("jeju", "제주", "제주특별자치도", 8, true);
        given(regionRepository.findAllByActiveTrueOrderByDisplayOrderAsc())
            .willReturn(List.of(seoul, jeju));

        List<RegionResponse> result = regionService.findAllActive();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCode()).isEqualTo("seoul");
        assertThat(result.get(0).getName()).isEqualTo("서울");
        assertThat(result.get(0).getDisplayOrder()).isEqualTo(1);
        assertThat(result.get(1).getCode()).isEqualTo("jeju");
    }
}
