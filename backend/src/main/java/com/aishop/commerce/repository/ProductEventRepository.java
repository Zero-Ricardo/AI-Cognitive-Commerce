package com.aishop.commerce.repository;

import com.aishop.commerce.domain.ProductEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.Instant;

public interface ProductEventRepository extends JpaRepository<ProductEvent, Long> {
    @Query("select e.product.id, sum(e.score) from ProductEvent e where (:categoryId is null or e.product.category.id = :categoryId or e.product.category.parentId = :categoryId) group by e.product.id order by sum(e.score) desc")
    List<Object[]> findHotProductScores(@Param("categoryId") Long categoryId);
    boolean existsByClientEventId(String clientEventId);
    boolean existsByUserIdAndProductIdAndEventTypeAndOccurredAtAfter(Long userId, Long productId,
            com.aishop.commerce.domain.Enums.ProductEventType eventType, Instant occurredAt);
    boolean existsByAnonymousIdAndProductIdAndEventTypeAndOccurredAtAfter(String anonymousId, Long productId,
            com.aishop.commerce.domain.Enums.ProductEventType eventType, Instant occurredAt);
}
