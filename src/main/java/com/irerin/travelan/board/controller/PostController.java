package com.irerin.travelan.board.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.irerin.travelan.board.dto.CreatePostCommand;
import com.irerin.travelan.board.dto.CreatePostRequest;
import com.irerin.travelan.board.dto.PostDetailResponse;
import com.irerin.travelan.board.dto.PostSummaryResponse;
import com.irerin.travelan.board.dto.UpdatePostCommand;
import com.irerin.travelan.board.dto.UpdatePostRequest;
import com.irerin.travelan.board.service.PostService;
import com.irerin.travelan.common.response.ApiResponse;
import com.irerin.travelan.common.response.PageMeta;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping("/api/v1/regions/{regionCode}/posts")
    public ApiResponse<List<PostSummaryResponse>> list(
        @PathVariable @Size(max = 50, message = "지역 코드는 50자 이하여야 합니다")
        @Pattern(regexp = "^[a-z][a-z0-9_]*$", message = "지역 코드는 영문 소문자로 시작하고 소문자/숫자/언더스코어만 사용할 수 있습니다") String regionCode,
        @RequestParam(defaultValue = "1") @Min(1) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Page<PostSummaryResponse> pageResult = postService.listByRegion(regionCode, page, size);

        return ApiResponse.<List<PostSummaryResponse>>builder()
            .data(pageResult.getContent())
            .page(PageMeta.from(pageResult, page))
            .build();
    }

    @GetMapping("/api/v1/posts")
    public ApiResponse<List<PostSummaryResponse>> listAll(
        @RequestParam(defaultValue = "1") @Min(1) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Page<PostSummaryResponse> pageResult = postService.listAll(page, size);

        return ApiResponse.<List<PostSummaryResponse>>builder()
            .data(pageResult.getContent())
            .page(PageMeta.from(pageResult, page))
            .build();
    }

    @GetMapping("/api/v1/posts/{postId}")
    public ApiResponse<PostDetailResponse> get(@PathVariable Long postId) {
        return ApiResponse.ok(postService.get(postId));
    }

    @PostMapping("/api/v1/regions/{regionCode}/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostDetailResponse> create(
        @PathVariable @Size(max = 50, message = "지역 코드는 50자 이하여야 합니다")
        @Pattern(regexp = "^[a-z][a-z0-9_]*$", message = "지역 코드는 영문 소문자로 시작하고 소문자/숫자/언더스코어만 사용할 수 있습니다") String regionCode,
        @Valid @RequestBody CreatePostRequest request,
        @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.ok(postService.create(CreatePostCommand.from(request, regionCode, userId)));
    }

    @PutMapping("/api/v1/posts/{postId}")
    public ApiResponse<PostDetailResponse> update(
        @PathVariable Long postId,
        @Valid @RequestBody UpdatePostRequest request,
        @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.ok(postService.update(UpdatePostCommand.from(request, postId, userId)));
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
