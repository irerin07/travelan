package com.irerin.travelan.board.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.irerin.travelan.board.dto.CreateReportRequest;
import com.irerin.travelan.board.dto.ReportPostCommand;
import com.irerin.travelan.board.dto.ReportResponse;
import com.irerin.travelan.board.entity.Post;
import com.irerin.travelan.board.entity.PostReport;
import com.irerin.travelan.board.entity.PostStatus;
import com.irerin.travelan.board.entity.Region;
import com.irerin.travelan.board.entity.ReportReason;
import com.irerin.travelan.board.repository.PostReportRepository;
import com.irerin.travelan.board.repository.PostRepository;
import com.irerin.travelan.common.exception.ForbiddenException;
import com.irerin.travelan.common.exception.NotFoundException;
import com.irerin.travelan.user.entity.User;
import com.irerin.travelan.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PostReportServiceTest {

    @Mock PostRepository postRepository;
    @Mock PostReportRepository postReportRepository;
    @Mock UserRepository userRepository;

    private PostReportService postReportService;

    private Region region;
    private User author;
    private User reporter;
    private Post post;

    @BeforeEach
    void setUp() {
        postReportService = new PostReportService(postRepository, postReportRepository, userRepository);

        region = Region.of("seoul", "서울", "desc", 1, true);
        ReflectionTestUtils.setField(region, "id", 10L);

        author = User.of("a@x.com", "p", "홍길동", "01000000000", "작성자");
        ReflectionTestUtils.setField(author, "id", 1L);

        reporter = User.of("b@x.com", "p", "신고자", "01011111111", "신고자닉");
        ReflectionTestUtils.setField(reporter, "id", 2L);

        post = Post.of(region, author, "제목", "내용");
        ReflectionTestUtils.setField(post, "id", 100L);
        ReflectionTestUtils.setField(post, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(post, "updatedAt", LocalDateTime.now());
    }

    private ReportPostCommand createCommand(Long postId, Long reporterId, ReportReason reason) {
        return ReportPostCommand.from(CreateReportRequest.from(reason), postId, reporterId);
    }

    @Test
    void report_succeeds_whenValidRequest() {
        given(postRepository.findByIdAndStatus(100L, PostStatus.PUBLISHED)).willReturn(Optional.of(post));
        given(userRepository.findById(2L)).willReturn(Optional.of(reporter));
        given(postReportRepository.save(any(PostReport.class))).willAnswer(inv -> {
            PostReport saved = inv.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 999L);
            ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.now());
            return saved;
        });

        ReportPostCommand command = createCommand(100L, 2L, ReportReason.SPAM);
        ReportResponse response = postReportService.report(command);

        assertThat(response.getReportId()).isEqualTo(999L);
        assertThat(response.getCreatedAt()).isNotNull();
        verify(postReportRepository).save(any(PostReport.class));
    }

    @Test
    void report_throwsNotFoundException_whenPostNotFound() {
        given(postRepository.findByIdAndStatus(999L, PostStatus.PUBLISHED)).willReturn(Optional.empty());

        ReportPostCommand command = createCommand(999L, 2L, ReportReason.SPAM);
        assertThatThrownBy(() -> postReportService.report(command))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void report_throwsNotFoundException_whenReporterNotFound() {
        given(postRepository.findByIdAndStatus(100L, PostStatus.PUBLISHED)).willReturn(Optional.of(post));
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        ReportPostCommand command = createCommand(100L, 99L, ReportReason.SPAM);
        assertThatThrownBy(() -> postReportService.report(command))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void report_throwsForbiddenException_whenReportingOwnPost() {
        given(postRepository.findByIdAndStatus(100L, PostStatus.PUBLISHED)).willReturn(Optional.of(post));

        // author (id=1) tries to report own post — self-report check happens before reporter lookup
        ReportPostCommand command = createCommand(100L, 1L, ReportReason.SPAM);
        assertThatThrownBy(() -> postReportService.report(command))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("자신의 게시글");
    }
}
