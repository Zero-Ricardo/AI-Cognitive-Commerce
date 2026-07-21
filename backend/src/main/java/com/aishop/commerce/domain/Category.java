package com.aishop.commerce.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter @Setter @Entity @Table(name = "categories")
public class Category {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "parent_id") private Long parentId;
    @Column(nullable = false, length = 80) private String name;
    @Column(nullable = false) private Integer level = 1;
    @Column(name = "sort_order", nullable = false) private Integer sortOrder = 0;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Enums.CommonStatus status = Enums.CommonStatus.ENABLED;
    @Column(nullable = false) private boolean deleted;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @PrePersist void create() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void update() { updatedAt = Instant.now(); }
}
