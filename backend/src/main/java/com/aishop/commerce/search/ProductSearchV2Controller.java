package com.aishop.commerce.search;

import com.aishop.commerce.catalog.CatalogDtos;
import com.aishop.commerce.common.ApiResponse;
import com.aishop.commerce.domain.Enums;
import com.aishop.commerce.repository.ProductRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/v2")
public class ProductSearchV2Controller {
    private final ElasticsearchProductSearchService search;
    private final ProductRepository products;
    public ProductSearchV2Controller(ElasticsearchProductSearchService search, ProductRepository products) {
        this.search = search; this.products = products;
    }

    @GetMapping("/products/search")
    public ApiResponse<ElasticsearchProductSearchService.SearchResponse> search(
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) Long categoryId, @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) BigDecimal priceMin, @RequestParam(required = false) BigDecimal priceMax,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(defaultValue = "RELEVANCE") CatalogDtos.ProductSort sort,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        return ApiResponse.ok(search.search(new CatalogDtos.ProductSearchCondition(keyword, categoryId, brandId,
                priceMin, priceMax, inStock, null, sort, page, pageSize)));
    }

    @GetMapping("/products/search/suggestions")
    public ApiResponse<List<ElasticsearchProductSearchService.Suggestion>> suggestions(
            @RequestParam @Size(max = 100) String keyword,
            @RequestParam(defaultValue = "8") @Min(1) @Max(20) int limit) {
        return ApiResponse.ok(search.suggestions(keyword, limit));
    }

    @PostMapping("/products:batch-summary")
    public ApiResponse<List<CatalogDtos.ProductView>> batchSummary(@RequestBody BatchSummaryRequest request) {
        List<Long> ordered = request.productIds().stream().distinct().limit(50).toList();
        Map<Long, com.aishop.commerce.domain.Product> values = products.findWithCategoryAndBrandByIdIn(ordered).stream()
                .filter(value -> !value.isDeleted() && value.getStatus() == Enums.ProductStatus.ON_SALE)
                .collect(java.util.stream.Collectors.toMap(com.aishop.commerce.domain.Product::getId, value -> value));
        return ApiResponse.ok(ordered.stream().map(values::get).filter(Objects::nonNull).map(CatalogDtos.ProductView::from).toList());
    }

    public record BatchSummaryRequest(List<Long> productIds) {
        public BatchSummaryRequest { productIds = productIds == null ? List.of() : productIds; }
    }
}
