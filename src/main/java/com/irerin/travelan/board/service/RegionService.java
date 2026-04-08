package com.irerin.travelan.board.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.irerin.travelan.board.dto.RegionResponse;
import com.irerin.travelan.board.repository.RegionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionService {

    private final RegionRepository regionRepository;

    public List<RegionResponse> findAllActive() {
        return regionRepository.findAllByActiveTrueOrderByDisplayOrderAsc().stream()
            .map(RegionResponse::from)
            .toList();
    }
}
