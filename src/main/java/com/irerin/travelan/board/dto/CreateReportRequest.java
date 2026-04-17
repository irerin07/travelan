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
    private CreateReportRequest(ReportReason reason) {
        this.reason = reason;
    }

    @JsonCreator
    public static CreateReportRequest from(@JsonProperty("reason") ReportReason reason) {
        return CreateReportRequest.builder().reason(reason).build();
    }
}
