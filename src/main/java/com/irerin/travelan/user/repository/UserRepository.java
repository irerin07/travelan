package com.irerin.travelan.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.irerin.travelan.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByNicknameIgnoreCase(String nickname);
    Optional<User> findByEmail(String email);
}
