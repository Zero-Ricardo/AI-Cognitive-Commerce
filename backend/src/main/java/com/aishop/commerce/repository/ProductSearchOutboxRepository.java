package com.aishop.commerce.repository;

import com.aishop.commerce.domain.ProductSearchOutbox;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ProductSearchOutboxRepository extends JpaRepository<ProductSearchOutbox, Long> {
    List<ProductSearchOutbox> findByStatusAndNextRetryAtBeforeOrderByIdAsc(
            ProductSearchOutbox.Status status, Instant nextRetryAt, Pageable pageable);
    List<ProductSearchOutbox> findByStatusAndNextRetryAtIsNullOrderByIdAsc(
            ProductSearchOutbox.Status status, Pageable pageable);
}
