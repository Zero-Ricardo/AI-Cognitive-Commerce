package com.aishop.commerce.catalog;

import com.aishop.commerce.common.ApiResponse;
import com.aishop.commerce.common.PageResult;
import com.aishop.commerce.domain.Enums;
import com.aishop.commerce.security.AppUserPrincipal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {
    private final ProductSearchService search;
    private final CatalogService catalog;
    private final HotProductService hotProducts;

    public CatalogController(ProductSearchService search, CatalogService catalog, HotProductService hotProducts) {
        this.search = search;
        this.catalog = catalog;
        this.hotProducts = hotProducts;
    }

    @GetMapping("/products")
    public ApiResponse<PageResult<CatalogDtos.ProductView>> products(
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) Long categoryId, @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) BigDecimal priceMin, @RequestParam(required = false) BigDecimal priceMax,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(defaultValue = "RELEVANCE") CatalogDtos.ProductSort sort,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        return ApiResponse.ok(search.search(new CatalogDtos.ProductSearchCondition(keyword, categoryId, brandId,
                priceMin, priceMax, inStock, null, sort, page, pageSize), false));
    }

    @GetMapping("/products/{id}")
    public ApiResponse<CatalogDtos.ProductView> product(@PathVariable Long id,
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestHeader(name = "X-Anonymous-Id", required = false) String anonymousId) {
        return ApiResponse.ok(catalog.publicProduct(id, principal == null ? null : principal.id(), anonymousId));
    }

    @GetMapping("/categories")
    public ApiResponse<List<CatalogDtos.CategoryView>> categories() { return ApiResponse.ok(catalog.publicCategories()); }

    @GetMapping("/brands")
    public ApiResponse<List<CatalogDtos.BrandView>> brands() { return ApiResponse.ok(catalog.publicBrands()); }

    @GetMapping("/products/hot")
    public ApiResponse<List<CatalogDtos.ProductView>> hotProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "8") @Min(1) @Max(20) int limit) {
        return ApiResponse.ok(hotProducts.getHotProducts(categoryId, limit));
    }
}
