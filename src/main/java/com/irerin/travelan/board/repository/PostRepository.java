package com.irerin.travelan.board.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.irerin.travelan.board.entity.Post;
import com.irerin.travelan.board.entity.PostStatus;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("select p from Post p join fetch p.author join fetch p.region where p.region.id = :regionId and p.status = :status")
    Page<Post> findByRegionIdAndStatus(Long regionId, PostStatus status, Pageable pageable);

    @Query("select p from Post p join fetch p.author join fetch p.region where p.id = :id and p.status = :status")
    Optional<Post> findByIdAndStatus(Long id, PostStatus status);
}
