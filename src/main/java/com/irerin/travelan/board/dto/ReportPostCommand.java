package com.irerin.travelan.board.dto;

import com.irerin.travelan.board.entity.Post;
import com.irerin.travelan.board.entity.PostReport;
import com.irerin.travelan.board.entity.ReportReason;
import com.irerin.travelan.user.entity.User;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class ReportPostCommand {

    private final Long postId;
    private final Long reporterId;
    private final ReportReason reason;

    @Builder(access = AccessLevel.PRIVATE)
    private ReportPostCommand(Long postId, Long reporterId, ReportReason reason) {
        this.postId = postId;
        this.reporterId = reporterId;
        this.reason = reason;
    }

    public static ReportPostCommand from(CreateReportRequest request, Long postId, Long reporterId) {
        return ReportPostCommand.builder()
            .postId(postId)
            .reporterId(reporterId)
            .reason(request.getReason())
            .build();
    }

    public static ReportPostCommand of(Long postId, Long reporterId, ReportReason reason) {
        return ReportPostCommand.builder()
            .postId(postId)
            .reporterId(reporterId)
            .reason(reason)
            .build();
    }

    public PostReport toEntity(Post post, User reporter) {
        return PostReport.of(post, reporter, reason);
    }
}
