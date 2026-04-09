package com.irerin.travelan.board.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UpdatePostCommand {

    private final Long postId;
    private final Long requesterId;
    private final String title;
    private final String content;

    public static UpdatePostCommand of(Long postId, Long requesterId, String title, String content) {
        return new UpdatePostCommand(postId, requesterId, title, content);
    }
}
