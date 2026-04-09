package com.irerin.travelan.board.dto;

import java.time.LocalDateTime;

import com.irerin.travelan.board.entity.Post;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PostDetailResponse {

    private final Long id;
    private final String regionCode;
    private final String regionName;
    private final String title;
    private final String content;
    private final String authorNickname;
    private final long viewCount;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static PostDetailResponse from(Post post) {
        return new PostDetailResponse(
            post.getId(),
            post.getRegion().getCode(),
            post.getRegion().getName(),
            post.getTitle(),
            post.getContent(),
            post.getAuthor().getNickname(),
            post.getViewCount(),
            post.getCreatedAt(),
            post.getUpdatedAt()
        );
    }
}
