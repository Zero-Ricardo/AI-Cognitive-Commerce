package com.aishop.commerce.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "user_browsing_history", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id"}))
public class UserBrowsingHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "product_id") private Product product;
    @Column(name = "view_count", nullable = false) private Integer viewCount = 1;
    @Column(name = "first_viewed_at", nullable = false) private Instant firstViewedAt;
    @Column(name = "last_viewed_at", nullable = false) private Instant lastViewedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (firstViewedAt == null) firstViewedAt = now;
        if (lastViewedAt == null) lastViewedAt = now;
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
