package com.aishop.commerce.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter @Setter @Entity @Table(name = "product_events")
public class ProductEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") private User user;
    @Column(name = "anonymous_id", length = 64) private String anonymousId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "product_id") private Product product;
    @Enumerated(EnumType.STRING) @Column(name = "event_type", nullable = false, length = 20) private Enums.ProductEventType eventType;
    @Column(nullable = false) private Integer score;
    @Column(name = "client_event_id", length = 64, unique = true) private String clientEventId;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @PrePersist void create() { occurredAt = Instant.now(); }
}
