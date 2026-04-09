package com.irerin.travelan.board.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.irerin.travelan.board.entity.PostHistory;

public interface PostHistoryRepository extends JpaRepository<PostHistory, Long> {

    List<PostHistory> findByPostIdOrderByCreatedAtDesc(Long postId);
}
