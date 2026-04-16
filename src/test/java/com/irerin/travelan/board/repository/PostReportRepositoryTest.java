package com.irerin.travelan.board.repository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.irerin.travelan.board.entity.Post;
import com.irerin.travelan.board.entity.PostReport;
import com.irerin.travelan.board.entity.Region;
import com.irerin.travelan.board.entity.ReportReason;
import com.irerin.travelan.common.config.JpaConfig;
import com.irerin.travelan.user.entity.User;
import com.irerin.travelan.user.repository.UserRepository;

@DataJpaTest
@Import(JpaConfig.class)
class PostReportRepositoryTest {

    @Autowired PostReportRepository postReportRepository;
    @Autowired PostRepository postRepository;
    @Autowired RegionRepository regionRepository;
    @Autowired UserRepository userRepository;

    private Post post;
    private User reporter;

    @BeforeEach
    void setUp() {
        Region region = regionRepository.save(Region.of("seoul", "서울", "설명", 1, true));
        User author = userRepository.save(User.of("author@test.com", "pass", "작성자", "01000000000", "작성자닉"));
        reporter = userRepository.save(User.of("reporter@test.com", "pass", "신고자", "01011111111", "신고자닉"));
        post = postRepository.save(Post.of(region, author, "제목", "내용"));
    }

    @Test
    void save_동일한_게시글_동일한_신고자_저장시_DataIntegrityViolationException_발생() {
        postReportRepository.saveAndFlush(PostReport.of(post, reporter, ReportReason.SPAM));

        assertThatThrownBy(() ->
            postReportRepository.saveAndFlush(PostReport.of(post, reporter, ReportReason.OBSCENE))
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}
