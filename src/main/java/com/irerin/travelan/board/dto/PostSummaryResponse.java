package com.irerin.travelan.board.dto;

import java.time.LocalDateTime;

import com.irerin.travelan.board.entity.Post;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PostSummaryResponse {

    private final Long id;
    private final String regionCode;
    private final String title;
    private final String authorNickname;
    private final long viewCount;
    private final LocalDateTime createdAt;

    public static PostSummaryResponse from(Post post) {
        return new PostSummaryResponse(
            post.getId(),
            post.getRegion().getCode(),
            post.getTitle(),
            post.getAuthor().getNickname(),
            post.getViewCount(),
            post.getCreatedAt()
        );
    }
}
