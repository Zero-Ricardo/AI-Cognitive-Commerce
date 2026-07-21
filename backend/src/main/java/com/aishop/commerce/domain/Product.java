package com.aishop.commerce.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter @Entity @Table(name = "products")
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "product_no", nullable = false, unique = true, length = 64) private String productNo;
    @Column(nullable = false, length = 200) private String name;
    @Column(length = 300) private String subtitle;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "category_id") private Category category;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "brand_id") private Brand brand;
    @Column(name = "sale_price", nullable = false, precision = 12, scale = 2) private BigDecimal salePrice;
    @Column(name = "original_price", precision = 12, scale = 2) private BigDecimal originalPrice;
    @Column(nullable = false) private Integer stock = 0;
    @Column(name = "main_image_url", nullable = false, length = 512) private String mainImageUrl;
    @Column(name = "image_urls_json", columnDefinition = "json") private String imageUrlsJson;
    @Column(nullable = false, columnDefinition = "text") private String description;
    @Column(length = 500) private String keywords;
    @Column(length = 500) private String scenarios;
    @Column(length = 500) private String audiences;
    @Column(name = "specification_json", columnDefinition = "json") private String specificationJson;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Enums.ProductStatus status = Enums.ProductStatus.DRAFT;
    @Column(name = "published_at") private Instant publishedAt;
    @Version private Integer version;
    @Column(nullable = false) private boolean deleted;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @PrePersist void create() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void update() { updatedAt = Instant.now(); }
}
