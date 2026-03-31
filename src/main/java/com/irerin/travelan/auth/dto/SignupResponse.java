package com.irerin.travelan.auth.dto;

import com.irerin.travelan.user.entity.User;

import lombok.Getter;

@Getter
public class SignupResponse {

    private final Long id;
    private final String email;
    private final String nickname;

    public SignupResponse(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.nickname = user.getNickname();
    }

}
