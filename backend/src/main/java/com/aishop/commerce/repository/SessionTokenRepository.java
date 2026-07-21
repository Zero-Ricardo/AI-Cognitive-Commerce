package com.aishop.commerce.repository;

import com.aishop.commerce.domain.SessionToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SessionTokenRepository extends JpaRepository<SessionToken, Long> {
    @Query("select distinct s from SessionToken s join fetch s.user u left join fetch u.roles where s.accessTokenHash = :hash and s.revoked = false")
    Optional<SessionToken> findByAccessTokenHashAndRevokedFalse(@Param("hash") String hash);

    @Query("select distinct s from SessionToken s join fetch s.user u left join fetch u.roles where s.refreshTokenHash = :hash and s.revoked = false")
    Optional<SessionToken> findByRefreshTokenHashAndRevokedFalse(@Param("hash") String hash);
}
