package com.irerin.travelan.board.dto;

import java.time.LocalDateTime;

import com.irerin.travelan.board.entity.Post;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class PostSummaryResponse {

    private final Long id;
    private final String regionCode;
    private final String title;
    private final String authorNickname;
    private final long viewCount;
    private final LocalDateTime createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private PostSummaryResponse(Long id, String regionCode, String title, String authorNickname, long viewCount, LocalDateTime createdAt) {
        this.id = id;
        this.regionCode = regionCode;
        this.title = title;
        this.authorNickname = authorNickname;
        this.viewCount = viewCount;
        this.createdAt = createdAt;
    }

    public static PostSummaryResponse from(Post post) {
        return PostSummaryResponse.builder()
            .id(post.getId())
            .regionCode(post.getRegion().getCode())
            .title(post.getTitle())
            .authorNickname(post.getAuthor().getNickname())
            .viewCount(post.getViewCount())
            .createdAt(post.getCreatedAt())
            .build();
    }
}
