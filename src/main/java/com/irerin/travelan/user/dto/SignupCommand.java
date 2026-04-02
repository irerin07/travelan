package com.irerin.travelan.user.dto;

import java.util.List;

import com.irerin.travelan.auth.dto.SignupRequest;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SignupCommand {

    private final String email;
    private final String password;
    private final String name;
    private final String phone;
    private final String nickname;
    private final List<String> interestRegions;

    public static SignupCommand of(String email, String password, String name, String phone, String nickname, List<String> interestRegions) {
        return new SignupCommand(email, password, name, phone, nickname, interestRegions);
    }

    public static SignupCommand from(SignupRequest request) {
        return new SignupCommand(
            request.getEmail(),
            request.getPassword(),
            request.getName(),
            request.getPhone(),
            request.getNickname(),
            request.getInterestRegions()
        );
    }
}
