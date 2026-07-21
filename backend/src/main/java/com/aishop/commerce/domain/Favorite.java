package com.aishop.commerce.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter @Setter @Entity @Table(name = "favorites", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id"}))
public class Favorite {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "product_id") private Product product;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @PrePersist void create() { createdAt = Instant.now(); }
}
