package com.aishop.commerce.repository;

import com.aishop.commerce.domain.Brand;
import com.aishop.commerce.domain.Enums;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BrandRepository extends JpaRepository<Brand, Long> {
    List<Brand> findByDeletedFalseAndStatusOrderBySortOrderAsc(Enums.CommonStatus status);
    List<Brand> findByDeletedFalseOrderBySortOrderAsc();
}
