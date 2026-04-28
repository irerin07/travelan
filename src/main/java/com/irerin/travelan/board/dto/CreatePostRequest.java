package com.irerin.travelan.board.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class CreatePostRequest {

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 150, message = "제목은 150자 이하여야 합니다")
    private String title;

    @NotBlank(message = "본문은 필수입니다")
    @Size(max = 10000, message = "본문은 10000자 이하여야 합니다")
    private String content;

    @Size(max = 10, message = "이미지는 최대 10장까지 첨부할 수 있습니다")
    private List<@Positive(message = "이미지 ID는 양수여야 합니다") Long> imageIds;

    public static CreatePostRequest of(String title, String content, List<Long> imageIds) {
        return CreatePostRequest.builder()
            .title(title)
            .content(content)
            .imageIds(imageIds)
            .build();
    }
}
