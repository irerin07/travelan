package com.irerin.travelan.user.dto;

import java.util.List;

import com.irerin.travelan.auth.dto.SignupRequest;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SignupCommand {

    private final String email;
    private final String password;
    private final String name;
    private final String phone;
    private final String nickname;
    private final List<String> interestRegions;

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
