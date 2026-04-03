package com.irerin.travelan.user.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.irerin.travelan.user.entity.User;
import com.irerin.travelan.user.entity.UserHistory;

public interface UserHistoryRepository extends JpaRepository<UserHistory, Long> {

    Page<UserHistory> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

}
