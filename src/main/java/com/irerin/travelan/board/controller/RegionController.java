package com.irerin.travelan.board.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.irerin.travelan.board.dto.RegionResponse;
import com.irerin.travelan.board.service.RegionService;
import com.irerin.travelan.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    @GetMapping
    public ApiResponse<List<RegionResponse>> list() {
        return ApiResponse.ok(regionService.findAllActive());
    }

}
