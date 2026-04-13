package com.irerin.travelan.board.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.irerin.travelan.board.entity.PostImage;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    List<PostImage> findByPostIdOrderByDisplayOrderAsc(Long postId);

    List<PostImage> findAllByIdInAndPostIsNull(List<Long> ids);

    List<PostImage> findAllByIdInAndPostIsNullAndUploaderId(List<Long> ids, Long uploaderId);

    List<PostImage> findByPostId(Long postId);
}
