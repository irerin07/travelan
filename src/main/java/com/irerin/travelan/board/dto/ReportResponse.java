package com.irerin.travelan.board.dto;

import java.time.LocalDateTime;

import com.irerin.travelan.board.entity.PostReport;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class ReportResponse {

    private final Long reportId;
    private final LocalDateTime createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private ReportResponse(Long reportId, LocalDateTime createdAt) {
        this.reportId = reportId;
        this.createdAt = createdAt;
    }

    public static ReportResponse from(PostReport report) {
        return ReportResponse.builder()
            .reportId(report.getId())
            .createdAt(report.getCreatedAt())
            .build();
    }

}
