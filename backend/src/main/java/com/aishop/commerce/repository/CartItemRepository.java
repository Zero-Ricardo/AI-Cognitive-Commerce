package com.aishop.commerce.repository;

import com.aishop.commerce.domain.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUserIdOrderByUpdatedAtDesc(Long userId);
    Optional<CartItem> findByIdAndUserId(Long id, Long userId);
    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);
    long countByUserId(Long userId);
}
