package com.irerin.travelan.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreatePostRequest {

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 150, message = "제목은 150자 이하여야 합니다")
    private String title;

    @NotBlank(message = "본문은 필수입니다")
    @Size(max = 10000, message = "본문은 10000자 이하여야 합니다")
    private String content;
}
