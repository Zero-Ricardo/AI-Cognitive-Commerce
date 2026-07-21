package com.aishop.commerce.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "product_search_outbox")
public class ProductSearchOutbox {
    public enum EventType { UPSERT, DELETE }
    public enum Status { NEW, PROCESSING, DONE, FAILED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "product_id", nullable = false) private Long productId;
    @Enumerated(EnumType.STRING) @Column(name = "event_type", nullable = false) private EventType eventType;
    @Column(name = "product_version", nullable = false) private Long productVersion;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status = Status.NEW;
    @Column(name = "retry_count", nullable = false) private Integer retryCount = 0;
    @Column(name = "next_retry_at") private Instant nextRetryAt;
    @Column(name = "last_error", length = 500) private String lastError;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "processed_at") private Instant processedAt;

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = Instant.now(); }
}
