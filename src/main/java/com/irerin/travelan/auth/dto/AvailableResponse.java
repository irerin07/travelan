package com.irerin.travelan.auth.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AvailableResponse {

    private final boolean available;

    public static AvailableResponse of(boolean available) {
        return new AvailableResponse(available);
    }

}
