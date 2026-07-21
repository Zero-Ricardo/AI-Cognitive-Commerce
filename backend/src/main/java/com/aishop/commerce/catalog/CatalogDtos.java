package com.aishop.commerce.catalog;

import com.aishop.commerce.domain.Brand;
import com.aishop.commerce.domain.Category;
import com.aishop.commerce.domain.Enums;
import com.aishop.commerce.domain.Product;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public final class CatalogDtos {
    private CatalogDtos() {}

    public enum ProductSort { RELEVANCE, PRICE_ASC, PRICE_DESC, NEWEST, POPULARITY, HOT }

    public record ProductSearchCondition(String keyword, Long categoryId, Long brandId,
                                         BigDecimal priceMin, BigDecimal priceMax, Boolean inStock,
                                         Enums.ProductStatus status, ProductSort sort,
                                         int page, int pageSize) {}

    public record CategoryView(Long id, Long parentId, String name, Integer level, Integer sortOrder,
                               Enums.CommonStatus status) {
        public static CategoryView from(Category value) {
            return new CategoryView(value.getId(), value.getParentId(), value.getName(), value.getLevel(),
                    value.getSortOrder(), value.getStatus());
        }
    }

    public record BrandView(Long id, String name, String logoUrl, String description, Integer sortOrder,
                            Enums.CommonStatus status) {
        public static BrandView from(Brand value) {
            return new BrandView(value.getId(), value.getName(), value.getLogoUrl(), value.getDescription(),
                    value.getSortOrder(), value.getStatus());
        }
    }

    public record ProductView(Long id, String productNo, String name, String subtitle,
                              CategoryView category, BrandView brand, BigDecimal salePrice,
                              BigDecimal originalPrice, Integer stock, String mainImageUrl,
                              String imageUrlsJson, String description, String keywords,
                              String scenarios, String audiences, String specificationJson,
                              Enums.ProductStatus status, Instant publishedAt, Integer version,
                              Instant createdAt, Instant updatedAt) {
        public static ProductView from(Product value) {
            return new ProductView(value.getId(), value.getProductNo(), value.getName(), value.getSubtitle(),
                    CategoryView.from(value.getCategory()), value.getBrand() == null ? null : BrandView.from(value.getBrand()),
                    value.getSalePrice(), value.getOriginalPrice(), value.getStock(), value.getMainImageUrl(),
                    value.getImageUrlsJson(), value.getDescription(), value.getKeywords(), value.getScenarios(),
                    value.getAudiences(), value.getSpecificationJson(), value.getStatus(), value.getPublishedAt(),
                    value.getVersion(), value.getCreatedAt(), value.getUpdatedAt());
        }
    }

    public record ProductSaveRequest(
            @NotBlank @Size(max = 64) String productNo,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 300) String subtitle,
            @NotNull Long categoryId,
            Long brandId,
            @NotNull @DecimalMin("0.00") BigDecimal salePrice,
            @DecimalMin("0.00") BigDecimal originalPrice,
            @NotNull @Min(0) Integer stock,
            @NotBlank @Size(max = 512) String mainImageUrl,
            String imageUrlsJson,
            @NotBlank String description,
            @Size(max = 500) String keywords,
            @Size(max = 500) String scenarios,
            @Size(max = 500) String audiences,
            String specificationJson,
            @NotNull Enums.ProductStatus status,
            Integer version) {}

    public record CategorySaveRequest(Long parentId, @NotBlank @Size(max = 80) String name,
                                      @Min(0) Integer sortOrder, @NotNull Enums.CommonStatus status) {}

    public record BrandSaveRequest(@NotBlank @Size(max = 80) String name, @Size(max = 512) String logoUrl,
                                   @Size(max = 500) String description, @Min(0) Integer sortOrder,
                                   @NotNull Enums.CommonStatus status) {}
}
