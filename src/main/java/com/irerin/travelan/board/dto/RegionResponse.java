package com.irerin.travelan.board.dto;

import com.irerin.travelan.board.entity.Region;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RegionResponse {

    private final String code;
    private final String name;
    private final String description;
    private final int displayOrder;

    public static RegionResponse from(Region region) {
        return new RegionResponse(
            region.getCode(),
            region.getName(),
            region.getDescription(),
            region.getDisplayOrder()
        );
    }
}
