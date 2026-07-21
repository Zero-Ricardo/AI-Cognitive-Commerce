package com.aishop.commerce.repository;

import com.aishop.commerce.domain.Enums;
import com.aishop.commerce.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Optional<Product> findByIdAndDeletedFalse(Long id);
    Optional<Product> findByIdAndDeletedFalseAndStatus(Long id, Enums.ProductStatus status);
    boolean existsByCategoryIdAndDeletedFalse(Long categoryId);
    boolean existsByBrandIdAndDeletedFalse(Long brandId);
    List<Product> findTop20ByDeletedFalseAndStatusOrderByPublishedAtDesc(Enums.ProductStatus status);
    @EntityGraph(attributePaths = {"category", "brand"})
    Optional<Product> findWithCategoryAndBrandById(Long id);
    @EntityGraph(attributePaths = {"category", "brand"})
    List<Product> findWithCategoryAndBrandByIdIn(List<Long> ids);
}
