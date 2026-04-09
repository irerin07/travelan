package com.irerin.travelan.board.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.irerin.travelan.board.dto.CreatePostRequest;
import com.irerin.travelan.board.dto.PostDetailResponse;
import com.irerin.travelan.board.dto.PostSummaryResponse;
import com.irerin.travelan.board.dto.UpdatePostRequest;
import com.irerin.travelan.board.service.PostService;
import com.irerin.travelan.common.response.ApiResponse;
import com.irerin.travelan.common.response.PageMeta;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping("/api/v1/regions/{regionCode}/posts")
    public ApiResponse<java.util.List<PostSummaryResponse>> list(
        @PathVariable String regionCode,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<PostSummaryResponse> page = postService.listByRegion(regionCode, pageable);
        return ApiResponse.<java.util.List<PostSummaryResponse>>builder()
            .data(page.getContent())
            .page(PageMeta.from(page, page.getNumber() + 1))
            .build();
    }

    @GetMapping("/api/v1/posts/{postId}")
    public ApiResponse<PostDetailResponse> get(@PathVariable Long postId) {
        return ApiResponse.ok(postService.get(postId));
    }

    @PostMapping("/api/v1/regions/{regionCode}/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostDetailResponse> create(
        @PathVariable String regionCode,
        @Valid @RequestBody CreatePostRequest request,
        @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.ok(postService.create(request.toCommand(regionCode, userId)));
    }

    @PutMapping("/api/v1/posts/{postId}")
    public ApiResponse<PostDetailResponse> update(
        @PathVariable Long postId,
        @Valid @RequestBody UpdatePostRequest request,
        @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.ok(postService.update(request.toCommand(postId, userId)));
    }

    @DeleteMapping("/api/v1/posts/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
        @PathVariable Long postId,
        @AuthenticationPrincipal Long userId
    ) {
        postService.delete(postId, userId);
    }
}
