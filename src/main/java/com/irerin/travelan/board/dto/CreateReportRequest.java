package com.irerin.travelan.board.dto;

import com.irerin.travelan.board.entity.ReportReason;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReportRequest {

    @NotNull(message = "신고 사유는 필수입니다")
    private ReportReason reason;
}
