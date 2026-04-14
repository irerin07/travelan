package com.irerin.travelan.board.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.irerin.travelan.board.dto.CreateReportRequest;
import com.irerin.travelan.board.dto.ReportPostCommand;
import com.irerin.travelan.board.dto.ReportResponse;
import com.irerin.travelan.board.service.PostReportService;
import com.irerin.travelan.common.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PostReportController {

    private final PostReportService postReportService;

    @PostMapping("/api/v1/posts/{postId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReportResponse> report(
        @PathVariable Long postId,
        @Valid @RequestBody CreateReportRequest request,
        @AuthenticationPrincipal Long userId
    ) {
        ReportPostCommand command = ReportPostCommand.from(request, postId, userId);
        return ApiResponse.ok(postReportService.report(command));
    }
}
