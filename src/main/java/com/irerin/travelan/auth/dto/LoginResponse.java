package com.irerin.travelan.auth.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LoginResponse {

    private final String accessToken;
    private final String tokenType;
    private final long expiresIn;

    public static LoginResponse of(String accessToken, String tokenType, long expiresIn) {
        return new LoginResponse(accessToken, tokenType, expiresIn);
    }

}
