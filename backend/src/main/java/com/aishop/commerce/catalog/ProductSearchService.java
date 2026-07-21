package com.aishop.commerce.catalog;

import com.aishop.commerce.common.PageResult;

public interface ProductSearchService {
    PageResult<CatalogDtos.ProductView> search(CatalogDtos.ProductSearchCondition condition, boolean admin);
}
