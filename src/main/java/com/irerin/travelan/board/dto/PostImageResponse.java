package com.irerin.travelan.board.dto;

import com.irerin.travelan.board.entity.PostImage;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class PostImageResponse {

    private final Long id;
    private final String url;

    @Builder(access = AccessLevel.PRIVATE)
    private PostImageResponse(Long id, String url) {
        this.id = id;
        this.url = url;
    }

    public static PostImageResponse from(PostImage image) {
        return PostImageResponse.builder()
            .id(image.getId())
            .url(image.getUrl())
            .build();
    }
}
