package com.irerin.travelan.user.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.irerin.travelan.user.entity.User;
import com.irerin.travelan.user.entity.UserHistory;

public interface UserHistoryRepository extends JpaRepository<UserHistory, Long> {

    List<UserHistory> findByUserOrderByCreatedAtDesc(User user);

}
