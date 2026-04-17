package com.irerin.travelan.user.dto;

import com.irerin.travelan.user.entity.User;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class MyPageResponse {

    private final Long id;
    private final String email;
    private final String nickname;
    private final String name;
    private final String phone;

    @Builder(access = AccessLevel.PRIVATE)
    private MyPageResponse(Long id, String email, String nickname, String name, String phone) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.name = name;
        this.phone = phone;
    }

    public static MyPageResponse from(User user) {
        return MyPageResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .nickname(user.getNickname())
            .name(user.getName())
            .phone(user.getPhone())
            .build();
    }
}
