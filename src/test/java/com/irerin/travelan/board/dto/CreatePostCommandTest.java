package com.irerin.travelan.board.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.irerin.travelan.board.entity.Post;
import com.irerin.travelan.board.entity.Region;
import com.irerin.travelan.user.entity.User;

class CreatePostCommandTest {

    @Test
    void toEntity_sanitizesContentAndBuildsPost() {
        Region region = Region.of("seoul", "서울", "desc", 1, true);
        ReflectionTestUtils.setField(region, "id", 1L);
        User author = User.of("a@x.com", "p", "홍길동", "01000000000", "여행자");
        ReflectionTestUtils.setField(author, "id", 1L);

        CreatePostRequest request = new CreatePostRequest();
        request.setTitle("제목");
        request.setContent("<p>본문</p><script>alert('x')</script>");
        CreatePostCommand command = CreatePostCommand.from(request, "seoul", 1L);

        Post post = command.toEntity(region, author);

        assertThat(post.getTitle()).isEqualTo("제목");
        assertThat(post.getContent()).contains("<p>본문</p>");
        assertThat(post.getContent()).doesNotContain("<script>");
        assertThat(post.getRegion()).isEqualTo(region);
        assertThat(post.getAuthor()).isEqualTo(author);
    }
}
