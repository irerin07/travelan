package com.irerin.travelan.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_interest_region")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserInterestRegion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String region;

    @Builder
    private UserInterestRegion(User user, String region) {
        this.user = user;
        this.region = region;
    }

    static UserInterestRegion of(User user, String region) {
        return UserInterestRegion.builder()
            .user(user)
            .region(region)
            .build();
    }

}
