package com.irerin.travelan.user.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "user_history")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserAction action;

    @Column(length = 50)
    private String field;

    @Column(length = 500)
    private String oldValue;

    @Column(length = 500)
    private String newValue;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private UserHistory(User user, UserAction action, String field, String oldValue, String newValue) {
        this.user = user;
        this.action = action;
        this.field = field;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public static UserHistory of(User user, UserAction action, String field, String oldValue, String newValue) {
        return UserHistory.builder()
            .user(user)
            .action(action)
            .field(field)
            .oldValue(oldValue)
            .newValue(newValue)
            .build();
    }

}
