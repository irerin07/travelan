package com.irerin.travelan.auth.dto;

import com.irerin.travelan.user.entity.User;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SignupResponse {

    private final Long id;
    private final String email;
    private final String nickname;

    public static SignupResponse from(User user) {
        return new SignupResponse(user.getId(), user.getEmail(), user.getNickname());
    }

}
