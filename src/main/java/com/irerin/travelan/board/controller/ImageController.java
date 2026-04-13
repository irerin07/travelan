package com.irerin.travelan.board.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.irerin.travelan.board.dto.PostImageResponse;
import com.irerin.travelan.board.dto.UploadImageCommand;
import com.irerin.travelan.board.service.ImageUploadService;
import com.irerin.travelan.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ImageController {

    private final ImageUploadService imageUploadService;

    @PostMapping("/api/v1/posts/images")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<PostImageResponse>> upload(
        @RequestParam("images") List<MultipartFile> files,
        @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.ok(imageUploadService.upload(UploadImageCommand.from(userId, files)));
    }
}
