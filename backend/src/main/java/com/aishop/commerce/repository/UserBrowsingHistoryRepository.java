package com.aishop.commerce.repository;

import com.aishop.commerce.domain.UserBrowsingHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserBrowsingHistoryRepository extends JpaRepository<UserBrowsingHistory, Long> {
    Optional<UserBrowsingHistory> findByUserIdAndProductId(Long userId, Long productId);
    Page<UserBrowsingHistory> findByUserId(Long userId, Pageable pageable);
    void deleteByUserIdAndProductId(Long userId, Long productId);
    void deleteByUserId(Long userId);
}
