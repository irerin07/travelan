package com.irerin.travelan.board.dto;

import com.irerin.travelan.board.entity.Region;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class RegionResponse {

    private final String code;
    private final String name;
    private final String description;
    private final int displayOrder;

    @Builder(access = AccessLevel.PRIVATE)
    private RegionResponse(String code, String name, String description, int displayOrder) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.displayOrder = displayOrder;
    }

    public static RegionResponse from(Region region) {
        return RegionResponse.builder()
            .code(region.getCode())
            .name(region.getName())
            .description(region.getDescription())
            .displayOrder(region.getDisplayOrder())
            .build();
    }
}
