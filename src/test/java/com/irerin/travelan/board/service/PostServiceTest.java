package com.irerin.travelan.board.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.irerin.travelan.board.dto.CreatePostCommand;
import com.irerin.travelan.board.dto.PostDetailResponse;
import com.irerin.travelan.board.dto.UpdatePostCommand;
import com.irerin.travelan.board.entity.Post;
import com.irerin.travelan.board.entity.PostStatus;
import com.irerin.travelan.board.entity.Region;
import com.irerin.travelan.board.repository.PostRepository;
import com.irerin.travelan.board.repository.RegionRepository;
import com.irerin.travelan.common.exception.ForbiddenException;
import com.irerin.travelan.common.exception.NotFoundException;
import com.irerin.travelan.user.entity.User;
import com.irerin.travelan.user.entity.UserRole;
import com.irerin.travelan.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock PostRepository postRepository;
    @Mock RegionRepository regionRepository;
    @Mock UserRepository userRepository;
    @InjectMocks PostService postService;

    private Region region;
    private User author;
    private User other;

    @BeforeEach
    void setUp() {
        region = Region.of("seoul", "서울", "desc", 1, true);
        ReflectionTestUtils.setField(region, "id", 10L);
        author = User.of("a@x.com", "p", "홍길동", "01000000000", "여행자");
        ReflectionTestUtils.setField(author, "id", 1L);
        other = User.of("b@x.com", "p", "김철수", "01011111111", "다른사람");
        ReflectionTestUtils.setField(other, "id", 2L);
    }

    @Test
    void create_sanitizesContentAndSavesPost() {
        given(regionRepository.findByCodeAndActiveTrue("seoul")).willReturn(Optional.of(region));
        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(postRepository.save(any(Post.class))).willAnswer(inv -> inv.getArgument(0));

        PostDetailResponse result = postService.create(
            CreatePostCommand.of("seoul", 1L, "title", "<p>ok</p><script>bad</script>")
        );

        assertThat(result.getTitle()).isEqualTo("title");
        assertThat(result.getContent()).doesNotContain("<script>");
        assertThat(result.getContent()).contains("<p>ok</p>");
    }

    @Test
    void create_throwsWhenRegionNotFound() {
        given(regionRepository.findByCodeAndActiveTrue("nope")).willReturn(Optional.empty());
        assertThatThrownBy(() -> postService.create(CreatePostCommand.of("nope", 1L, "t", "c")))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void update_throwsForbiddenWhenNotAuthor() {
        Post post = Post.of(region, author, "t", "c");
        ReflectionTestUtils.setField(post, "id", 5L);
        given(postRepository.findByIdAndStatus(5L, PostStatus.PUBLISHED)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.update(UpdatePostCommand.of(5L, 2L, "t2", "c2")))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void update_succeedsForAuthor() {
        Post post = Post.of(region, author, "t", "c");
        ReflectionTestUtils.setField(post, "id", 5L);
        given(postRepository.findByIdAndStatus(5L, PostStatus.PUBLISHED)).willReturn(Optional.of(post));

        PostDetailResponse result = postService.update(UpdatePostCommand.of(5L, 1L, "new", "<b>hi</b>"));
        assertThat(result.getTitle()).isEqualTo("new");
        assertThat(result.getContent()).contains("<b>hi</b>");
    }

    @Test
    void delete_allowsAdminEvenIfNotAuthor() {
        Post post = Post.of(region, author, "t", "c");
        ReflectionTestUtils.setField(post, "id", 5L);
        ReflectionTestUtils.setField(other, "role", UserRole.ADMIN);
        given(postRepository.findByIdAndStatus(5L, PostStatus.PUBLISHED)).willReturn(Optional.of(post));
        given(userRepository.findById(2L)).willReturn(Optional.of(other));

        postService.delete(5L, 2L);

        assertThat(post.getStatus()).isEqualTo(PostStatus.DELETED);
    }

    @Test
    void delete_throwsForbiddenForNonAuthorNonAdmin() {
        Post post = Post.of(region, author, "t", "c");
        ReflectionTestUtils.setField(post, "id", 5L);
        given(postRepository.findByIdAndStatus(5L, PostStatus.PUBLISHED)).willReturn(Optional.of(post));
        given(userRepository.findById(2L)).willReturn(Optional.of(other));

        assertThatThrownBy(() -> postService.delete(5L, 2L))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void get_increasesViewCount() {
        Post post = Post.of(region, author, "t", "c");
        ReflectionTestUtils.setField(post, "id", 5L);
        given(postRepository.findByIdAndStatus(5L, PostStatus.PUBLISHED)).willReturn(Optional.of(post));

        postService.get(5L);

        assertThat(post.getViewCount()).isEqualTo(1L);
    }
}
