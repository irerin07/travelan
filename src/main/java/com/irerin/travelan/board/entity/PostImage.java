package com.irerin.travelan.board.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
@Table(name = "post_images")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(nullable = false)
    private long size;

    @Column(name = "uploader_id", nullable = false)
    private Long uploaderId;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private PostImage(String url, String originalName, long size, Long uploaderId) {
        this.url = url;
        this.originalName = originalName;
        this.size = size;
        this.uploaderId = uploaderId;
        this.displayOrder = 0;
    }

    public static PostImage of(String url, String originalName, long size, Long uploaderId) {
        return PostImage.builder()
            .url(url)
            .originalName(originalName)
            .size(size)
            .uploaderId(uploaderId)
            .build();
    }

    public void attachTo(Post post, int order) {
        this.post = post;
        this.displayOrder = order;
    }

    public void detach() {
        this.post = null;
        this.displayOrder = 0;
    }
}
