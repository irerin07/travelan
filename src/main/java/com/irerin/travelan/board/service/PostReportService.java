package com.irerin.travelan.board.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.irerin.travelan.board.dto.ReportPostCommand;
import com.irerin.travelan.board.dto.ReportResponse;
import com.irerin.travelan.board.entity.Post;
import com.irerin.travelan.board.entity.PostReport;
import com.irerin.travelan.board.entity.PostStatus;
import com.irerin.travelan.board.repository.PostReportRepository;
import com.irerin.travelan.board.repository.PostRepository;
import com.irerin.travelan.common.exception.DuplicateException;
import com.irerin.travelan.common.exception.ForbiddenException;
import com.irerin.travelan.common.exception.NotFoundException;
import com.irerin.travelan.user.entity.User;
import com.irerin.travelan.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostReportService {

    private final PostRepository postRepository;
    private final PostReportRepository postReportRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReportResponse report(ReportPostCommand command) {
        Post post = postRepository.findByIdAndStatus(command.getPostId(), PostStatus.PUBLISHED)
            .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다"));

        if (post.isAuthor(command.getReporterId())) {
            throw new ForbiddenException("자신의 게시글은 신고할 수 없습니다");
        }

        if (postReportRepository.existsByPostIdAndReporterId(command.getPostId(), command.getReporterId())) {
            throw new DuplicateException("이미 신고한 게시글입니다");
        }

        User reporter = userRepository.findById(command.getReporterId())
            .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

        PostReport saved = postReportRepository.save(command.toEntity(post, reporter));
        return ReportResponse.from(saved);
    }
}
