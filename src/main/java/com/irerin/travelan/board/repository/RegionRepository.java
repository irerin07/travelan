package com.irerin.travelan.board.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.irerin.travelan.board.entity.Region;

public interface RegionRepository extends JpaRepository<Region, Long> {

    List<Region> findAllByActiveTrueOrderByDisplayOrderAsc();

    Optional<Region> findByCodeAndActiveTrue(String code);
}
