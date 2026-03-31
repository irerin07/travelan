package com.irerin.travelan.admin.dto;

import java.time.LocalDateTime;

import com.irerin.travelan.user.entity.User;
import com.irerin.travelan.user.entity.UserRole;
import com.irerin.travelan.user.entity.UserStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserSummaryResponse {

    private final Long id;
    private final String email;
    private final String name;
    private final String phone;
    private final String nickname;
    private final UserStatus status;
    private final UserRole role;
    private final LocalDateTime createdAt;

    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getPhone(),
            user.getNickname(),
            user.getStatus(),
            user.getRole(),
            user.getCreatedAt()
        );
    }
}
