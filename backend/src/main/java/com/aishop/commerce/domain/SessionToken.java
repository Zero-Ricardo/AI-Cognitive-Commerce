package com.aishop.commerce.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "session_tokens")
public class SessionToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;
    @Column(name = "access_token_hash", nullable = false, unique = true, length = 64, columnDefinition = "char(64)")
    private String accessTokenHash;
    @Column(name = "refresh_token_hash", nullable = false, unique = true, length = 64, columnDefinition = "char(64)")
    private String refreshTokenHash;
    @Column(name = "access_expires_at", nullable = false)
    private Instant accessExpiresAt;
    @Column(name = "refresh_expires_at", nullable = false)
    private Instant refreshExpiresAt;
    @Column(nullable = false)
    private boolean revoked;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist void create() { createdAt = Instant.now(); }
}
