package com.irerin.travelan.auth.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LoginCommand {

    private final String email;
    private final String password;

    public static LoginCommand from(LoginRequest request) {
        return new LoginCommand(request.getEmail(), request.getPassword());
    }

}
