package com.aishop.commerce.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter @Setter @Entity @Table(name = "brands")
public class Brand {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 80) private String name;
    @Column(name = "logo_url", length = 512) private String logoUrl;
    @Column(length = 500) private String description;
    @Column(name = "sort_order", nullable = false) private Integer sortOrder = 0;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Enums.CommonStatus status = Enums.CommonStatus.ENABLED;
    @Column(nullable = false) private boolean deleted;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @PrePersist void create() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void update() { updatedAt = Instant.now(); }
}
