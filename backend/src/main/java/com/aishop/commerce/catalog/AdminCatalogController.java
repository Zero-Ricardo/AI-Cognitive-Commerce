package com.aishop.commerce.catalog;

import com.aishop.commerce.common.ApiResponse;
import com.aishop.commerce.common.PageResult;
import com.aishop.commerce.domain.Enums;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminCatalogController {
    private final ProductSearchService search;
    private final CatalogService catalog;

    public AdminCatalogController(ProductSearchService search, CatalogService catalog) {
        this.search = search;
        this.catalog = catalog;
    }

    @GetMapping("/products")
    public ApiResponse<PageResult<CatalogDtos.ProductView>> products(
            @RequestParam(required = false) String keyword, @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId, @RequestParam(required = false) BigDecimal priceMin,
            @RequestParam(required = false) BigDecimal priceMax, @RequestParam(required = false) Enums.ProductStatus status,
            @RequestParam(defaultValue = "NEWEST") CatalogDtos.ProductSort sort,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(search.search(new CatalogDtos.ProductSearchCondition(keyword, categoryId, brandId,
                priceMin, priceMax, null, status, sort, page, pageSize), true));
    }

    @GetMapping("/products/{id}")
    public ApiResponse<CatalogDtos.ProductView> product(@PathVariable Long id) {
        return ApiResponse.ok(catalog.adminProduct(id));
    }

    @PostMapping("/products")
    public ApiResponse<CatalogDtos.ProductView> create(@Valid @RequestBody CatalogDtos.ProductSaveRequest request) {
        return ApiResponse.ok(catalog.create(request));
    }

    @PutMapping("/products/{id}")
    public ApiResponse<CatalogDtos.ProductView> update(@PathVariable Long id,
            @Valid @RequestBody CatalogDtos.ProductSaveRequest request) {
        return ApiResponse.ok(catalog.update(id, request));
    }

    @PatchMapping("/products/{id}/status")
    public ApiResponse<CatalogDtos.ProductView> status(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return ApiResponse.ok(catalog.changeStatus(id, request.status(), request.version()));
    }

    @DeleteMapping("/products/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @RequestParam Integer version) {
        catalog.deleteProduct(id, version);
        return ApiResponse.ok(null);
    }

    @GetMapping("/categories")
    public ApiResponse<List<CatalogDtos.CategoryView>> categories() { return ApiResponse.ok(catalog.adminCategories()); }
    @PostMapping("/categories")
    public ApiResponse<CatalogDtos.CategoryView> createCategory(@Valid @RequestBody CatalogDtos.CategorySaveRequest request) {
        return ApiResponse.ok(catalog.saveCategory(null, request));
    }
    @PutMapping("/categories/{id}")
    public ApiResponse<CatalogDtos.CategoryView> updateCategory(@PathVariable Long id,
            @Valid @RequestBody CatalogDtos.CategorySaveRequest request) {
        return ApiResponse.ok(catalog.saveCategory(id, request));
    }
    @DeleteMapping("/categories/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) { catalog.deleteCategory(id); return ApiResponse.ok(null); }

    @GetMapping("/brands")
    public ApiResponse<List<CatalogDtos.BrandView>> brands() { return ApiResponse.ok(catalog.adminBrands()); }
    @PostMapping("/brands")
    public ApiResponse<CatalogDtos.BrandView> createBrand(@Valid @RequestBody CatalogDtos.BrandSaveRequest request) {
        return ApiResponse.ok(catalog.saveBrand(null, request));
    }
    @PutMapping("/brands/{id}")
    public ApiResponse<CatalogDtos.BrandView> updateBrand(@PathVariable Long id,
            @Valid @RequestBody CatalogDtos.BrandSaveRequest request) {
        return ApiResponse.ok(catalog.saveBrand(id, request));
    }
    @DeleteMapping("/brands/{id}")
    public ApiResponse<Void> deleteBrand(@PathVariable Long id) { catalog.deleteBrand(id); return ApiResponse.ok(null); }

    public record StatusRequest(Enums.ProductStatus status, Integer version) {}
}
