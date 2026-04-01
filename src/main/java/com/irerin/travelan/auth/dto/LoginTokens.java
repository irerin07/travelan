package com.irerin.travelan.auth.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LoginTokens {

    private final String accessToken;
    private final String refreshToken;
    private final long accessTokenExpiry;

    public static LoginTokens of(String accessToken, String refreshToken, long accessTokenExpiry) {
        return new LoginTokens(accessToken, refreshToken, accessTokenExpiry);
    }

}
