package com.irerin.travelan.board.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class CreatePostCommand {

    private final String regionCode;
    private final Long requesterId;
    private final String title;
    private final String content;

    @Builder(access = AccessLevel.PRIVATE)
    private CreatePostCommand(String regionCode, Long requesterId, String title, String content) {
        this.regionCode = regionCode;
        this.requesterId = requesterId;
        this.title = title;
        this.content = content;
    }

    public static CreatePostCommand from(CreatePostRequest request, String regionCode, Long requesterId) {
        return CreatePostCommand.builder()
            .regionCode(regionCode)
            .requesterId(requesterId)
            .title(request.getTitle())
            .content(request.getContent())
            .build();
    }
}
