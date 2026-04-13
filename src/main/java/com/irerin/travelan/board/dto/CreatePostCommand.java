package com.irerin.travelan.board.dto;

import java.util.List;

import com.irerin.travelan.board.entity.Post;
import com.irerin.travelan.board.entity.Region;
import com.irerin.travelan.board.support.HtmlSanitizer;
import com.irerin.travelan.user.entity.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class CreatePostCommand {

    private final String regionCode;
    private final Long requesterId;
    private final String title;
    private final String content;
    private final List<Long> imageIds;

    @Builder(access = AccessLevel.PRIVATE)
    private CreatePostCommand(String regionCode, Long requesterId, String title, String content, List<Long> imageIds) {
        this.regionCode = regionCode;
        this.requesterId = requesterId;
        this.title = title;
        this.content = content;
        this.imageIds = imageIds;
    }

    public static CreatePostCommand from(CreatePostRequest request, String regionCode, Long requesterId) {
        return CreatePostCommand.builder()
            .regionCode(regionCode)
            .requesterId(requesterId)
            .title(request.getTitle())
            .content(request.getContent())
            .imageIds(request.getImageIds())
            .build();
    }

    public Post toEntity(Region region, User author) {
        return Post.of(region, author, title, HtmlSanitizer.sanitize(content));
    }

}
