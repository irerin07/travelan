package com.irerin.travelan.board.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class UpdatePostCommand {

    private final Long postId;
    private final Long requesterId;
    private final String title;
    private final String content;

    @Builder(access = AccessLevel.PRIVATE)
    private UpdatePostCommand(Long postId, Long requesterId, String title, String content) {
        this.postId = postId;
        this.requesterId = requesterId;
        this.title = title;
        this.content = content;
    }

    public static UpdatePostCommand from(UpdatePostRequest request, Long postId, Long requesterId) {
        return UpdatePostCommand.builder()
            .postId(postId)
            .requesterId(requesterId)
            .title(request.getTitle())
            .content(request.getContent())
            .build();
    }
}
