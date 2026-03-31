package com.irerin.travelan.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.irerin.travelan.user.entity.User;
import com.irerin.travelan.user.entity.UserStatus;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmailAndStatusNot(String email, UserStatus status);

    boolean existsByPhoneAndStatusNot(String phone, UserStatus status);

    boolean existsByNicknameIgnoreCaseAndStatusNot(String nickname, UserStatus status);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    Optional<User> findByNicknameIgnoreCase(String nickname);

}
