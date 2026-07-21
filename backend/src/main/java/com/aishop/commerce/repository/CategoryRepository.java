package com.aishop.commerce.repository;

import com.aishop.commerce.domain.Category;
import com.aishop.commerce.domain.Enums;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByDeletedFalseAndStatusOrderBySortOrderAsc(Enums.CommonStatus status);
    List<Category> findByDeletedFalseOrderBySortOrderAsc();
    boolean existsByParentIdAndDeletedFalse(Long parentId);
}
