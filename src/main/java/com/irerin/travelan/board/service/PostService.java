package com.irerin.travelan.board.service;

import java.time.Clock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.irerin.travelan.board.dto.CreatePostCommand;
import com.irerin.travelan.board.dto.PostDetailResponse;
import com.irerin.travelan.board.dto.PostSummaryResponse;
import com.irerin.travelan.board.dto.UpdatePostCommand;
import com.irerin.travelan.board.entity.Post;
import com.irerin.travelan.board.entity.PostHistory;
import com.irerin.travelan.board.entity.PostHistoryAction;
import com.irerin.travelan.board.entity.PostStatus;
import com.irerin.travelan.board.entity.Region;
import com.irerin.travelan.board.repository.PostHistoryRepository;
import com.irerin.travelan.board.repository.PostRepository;
import com.irerin.travelan.board.repository.RegionRepository;
import com.irerin.travelan.board.support.HtmlSanitizer;
import com.irerin.travelan.common.exception.ForbiddenException;
import com.irerin.travelan.common.exception.NotFoundException;
import com.irerin.travelan.user.entity.User;
import com.irerin.travelan.user.entity.UserRole;
import com.irerin.travelan.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostHistoryRepository postHistoryRepository;
    private final RegionRepository regionRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional
    public PostDetailResponse create(CreatePostCommand command) {
        Region region = regionRepository.findByCodeAndActiveTrue(command.getRegionCode())
            .orElseThrow(() -> new NotFoundException("지역을 찾을 수 없습니다"));

        User author = userRepository.findById(command.getRequesterId())
            .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

        Post post = command.toEntity(region, author);

        return PostDetailResponse.from(postRepository.save(post));
    }

    public Page<PostSummaryResponse> listByRegion(String regionCode, Pageable pageable) {
        Region region = regionRepository.findByCodeAndActiveTrue(regionCode)
            .orElseThrow(() -> new NotFoundException("지역을 찾을 수 없습니다"));
        return postRepository.findByRegionIdAndStatus(region.getId(), PostStatus.PUBLISHED, pageable)
            .map(PostSummaryResponse::from);
    }

    @Transactional
    public PostDetailResponse get(Long postId) {
        Post post = postRepository.findByIdAndStatus(postId, PostStatus.PUBLISHED)
            .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다"));
        post.increaseViewCount();
        return PostDetailResponse.from(post);
    }

    @Transactional
    public PostDetailResponse update(UpdatePostCommand command) {
        Post post = postRepository.findByIdAndStatus(command.getPostId(), PostStatus.PUBLISHED)
            .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다"));

        if (!post.canModify(command.getRequesterId())) {
            throw new ForbiddenException("게시글 수정 권한이 없습니다");
        }

        postHistoryRepository.save(PostHistory.snapshot(post, PostHistoryAction.UPDATED, command.getRequesterId()));
        post.update(command.getTitle(), HtmlSanitizer.sanitize(command.getContent()));

        return PostDetailResponse.from(post);
    }

    @Transactional
    public void delete(Long postId, Long requesterId) {
        Post post = postRepository.findByIdAndStatus(postId, PostStatus.PUBLISHED)
            .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다"));

        UserRole role = userRepository.findById(requesterId)
            .map(User::getRole)
            .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

        if (!post.canDelete(requesterId, role)) {
            throw new ForbiddenException("게시글 삭제 권한이 없습니다");
        }

        postHistoryRepository.save(PostHistory.snapshot(post, PostHistoryAction.DELETED, requesterId));
        post.delete(clock);
    }

}
