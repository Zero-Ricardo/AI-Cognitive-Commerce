package com.aishop.commerce.common;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResult<T>(List<T> items, int page, int pageSize, long total, int totalPages) {
    public static <T> PageResult<T> from(Page<T> source) {
        return new PageResult<>(source.getContent(), source.getNumber() + 1, source.getSize(),
                source.getTotalElements(), source.getTotalPages());
    }
}
