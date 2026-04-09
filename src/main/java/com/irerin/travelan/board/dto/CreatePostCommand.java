package com.irerin.travelan.board.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CreatePostCommand {

    private final String regionCode;
    private final Long authorId;
    private final String title;
    private final String content;

    public static CreatePostCommand of(String regionCode, Long authorId, String title, String content) {
        return new CreatePostCommand(regionCode, authorId, title, content);
    }
}
