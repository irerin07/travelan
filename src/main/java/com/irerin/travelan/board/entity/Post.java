package com.irerin.travelan.board.entity;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.irerin.travelan.user.entity.User;
import com.irerin.travelan.user.entity.UserRole;

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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "posts")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 150)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Post(Region region, User author, String title, String content) {
        this.region = region;
        this.author = author;
        this.title = title;
        this.content = content;
        this.viewCount = 0L;
        this.status = PostStatus.PUBLISHED;
    }

    public static Post of(Region region, User author, String title, String content) {
        return Post.builder()
            .region(region)
            .author(author)
            .title(title)
            .content(content)
            .build();
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void delete(Clock clock) {
        this.status = PostStatus.DELETED;
        this.deletedAt = LocalDateTime.now(clock);
    }

    public boolean isVisible() {
        return this.status == PostStatus.PUBLISHED;
    }

    public boolean isAuthor(Long userId) {
        return this.author.getId().equals(userId);
    }

    public boolean canModify(Long userId) {
        return isAuthor(userId);
    }

    public boolean canDelete(Long userId, UserRole role) {
        return isAuthor(userId) || role == UserRole.ADMIN;
    }

}
