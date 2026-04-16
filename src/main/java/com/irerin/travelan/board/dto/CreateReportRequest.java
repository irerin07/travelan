package com.irerin.travelan.board.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.irerin.travelan.board.entity.ReportReason;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class CreateReportRequest {

    @NotNull(message = "신고 사유는 필수입니다")
    private final ReportReason reason;

    @Builder(access = AccessLevel.PRIVATE)
    @JsonCreator
    private CreateReportRequest(@JsonProperty("reason") ReportReason reason) {
        this.reason = reason;
    }

    public static CreateReportRequest of(ReportReason reason) {
        return CreateReportRequest.builder()
            .reason(reason)
            .build();
    }
}
