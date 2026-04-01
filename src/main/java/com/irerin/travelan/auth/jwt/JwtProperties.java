package com.irerin.travelan.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;
    private long accessTokenExpiry;   // 초 단위
    private long refreshTokenExpiry;  // 초 단위

}
