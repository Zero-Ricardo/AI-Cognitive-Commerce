package com.aishop.commerce.catalog;

import java.util.List;

public interface HotProductService {
    List<CatalogDtos.ProductView> getHotProducts(Long categoryId, int limit);
}
