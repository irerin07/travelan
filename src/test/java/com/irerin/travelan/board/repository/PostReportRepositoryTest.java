package com.irerin.travelan.board.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PostReportRepositoryTest {

    @Mock
    PostReportRepository postReportRepository;

    @Test
    void existsByPostIdAndReporterId_returnsTrueWhenExists() {
        given(postReportRepository.existsByPostIdAndReporterId(1L, 2L)).willReturn(true);

        assertThat(postReportRepository.existsByPostIdAndReporterId(1L, 2L)).isTrue();
    }

    @Test
    void existsByPostIdAndReporterId_returnsFalseWhenNotExists() {
        given(postReportRepository.existsByPostIdAndReporterId(1L, 2L)).willReturn(false);

        assertThat(postReportRepository.existsByPostIdAndReporterId(1L, 2L)).isFalse();
    }
}
