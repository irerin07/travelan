package com.irerin.travelan.board.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.irerin.travelan.board.entity.Post;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class PostDetailResponse {

    private final Long id;
    private final String regionCode;
    private final String regionName;
    private final String title;
    private final String content;
    private final String authorNickname;
    private final long viewCount;
    private final List<PostImageResponse> images;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private PostDetailResponse(Long id, String regionCode, String regionName, String title, String content,
                               String authorNickname, long viewCount, List<PostImageResponse> images,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.regionCode = regionCode;
        this.regionName = regionName;
        this.title = title;
        this.content = content;
        this.authorNickname = authorNickname;
        this.viewCount = viewCount;
        this.images = images;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PostDetailResponse from(Post post, List<PostImageResponse> images) {
        return PostDetailResponse.builder()
            .id(post.getId())
            .regionCode(post.getRegion().getCode())
            .regionName(post.getRegion().getName())
            .title(post.getTitle())
            .content(post.getContent())
            .authorNickname(post.getAuthor().getNickname())
            .viewCount(post.getViewCount())
            .images(images)
            .createdAt(post.getCreatedAt())
            .updatedAt(post.getUpdatedAt())
            .build();
    }
}
