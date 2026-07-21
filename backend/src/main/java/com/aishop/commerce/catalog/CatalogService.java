package com.aishop.commerce.catalog;

import com.aishop.commerce.common.BusinessException;
import com.aishop.commerce.domain.*;
import com.aishop.commerce.repository.*;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import com.aishop.commerce.search.ProductIndexOutboxService;

@Service
public class CatalogService {
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final BrandRepository brands;
    private final ProductIndexOutboxService searchOutbox;

    public CatalogService(ProductRepository products, CategoryRepository categories, BrandRepository brands,
                          ProductIndexOutboxService searchOutbox) {
        this.products = products;
        this.categories = categories;
        this.brands = brands;
        this.searchOutbox = searchOutbox;
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.CategoryView> publicCategories() {
        return categories.findByDeletedFalseAndStatusOrderBySortOrderAsc(Enums.CommonStatus.ENABLED)
                .stream().map(CatalogDtos.CategoryView::from).toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.BrandView> publicBrands() {
        return brands.findByDeletedFalseAndStatusOrderBySortOrderAsc(Enums.CommonStatus.ENABLED)
                .stream().map(CatalogDtos.BrandView::from).toList();
    }

    @Transactional(readOnly = true)
    public CatalogDtos.ProductView publicProduct(Long id, Long userId, String anonymousId) {
        Product product = products.findByIdAndDeletedFalseAndStatus(id, Enums.ProductStatus.ON_SALE)
                .orElseThrow(() -> BusinessException.notFound("商品不存在或已下架"));
        return CatalogDtos.ProductView.from(product);
    }

    @Transactional(readOnly = true)
    public CatalogDtos.ProductView adminProduct(Long id) {
        return CatalogDtos.ProductView.from(requiredProduct(id));
    }

    @Transactional
    public CatalogDtos.ProductView create(CatalogDtos.ProductSaveRequest request) {
        Product product = new Product();
        apply(product, request);
        product = products.saveAndFlush(product);
        searchOutbox.enqueue(product, ProductSearchOutbox.EventType.UPSERT);
        return CatalogDtos.ProductView.from(product);
    }

    @Transactional
    public CatalogDtos.ProductView update(Long id, CatalogDtos.ProductSaveRequest request) {
        Product product = requiredProduct(id);
        if (request.version() == null || !request.version().equals(product.getVersion())) {
            throw BusinessException.conflict("RESOURCE_CONFLICT", "商品已被其他管理员修改，请刷新后重试");
        }
        apply(product, request);
        try {
            product = products.saveAndFlush(product);
            searchOutbox.enqueue(product, ProductSearchOutbox.EventType.UPSERT);
            return CatalogDtos.ProductView.from(product);
        } catch (OptimisticLockingFailureException ex) {
            throw BusinessException.conflict("RESOURCE_CONFLICT", "商品已被其他管理员修改，请刷新后重试");
        }
    }

    @Transactional
    public CatalogDtos.ProductView changeStatus(Long id, Enums.ProductStatus status, Integer version) {
        Product product = requiredProduct(id);
        if (version == null || !version.equals(product.getVersion())) {
            throw BusinessException.conflict("RESOURCE_CONFLICT", "商品版本已变化，请刷新后重试");
        }
        if (status == Enums.ProductStatus.ON_SALE) validatePublish(product);
        product.setStatus(status);
        if (status == Enums.ProductStatus.ON_SALE && product.getPublishedAt() == null) product.setPublishedAt(Instant.now());
        product = products.saveAndFlush(product);
        searchOutbox.enqueue(product, ProductSearchOutbox.EventType.UPSERT);
        return CatalogDtos.ProductView.from(product);
    }

    @Transactional
    public void deleteProduct(Long id, Integer version) {
        Product product = requiredProduct(id);
        if (version == null || !version.equals(product.getVersion())) {
            throw BusinessException.conflict("RESOURCE_CONFLICT", "商品版本已变化，请刷新后重试");
        }
        product.setDeleted(true);
        product.setStatus(Enums.ProductStatus.OFF_SALE);
        product = products.saveAndFlush(product);
        searchOutbox.enqueue(product, ProductSearchOutbox.EventType.DELETE);
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.CategoryView> adminCategories() {
        return categories.findByDeletedFalseOrderBySortOrderAsc().stream().map(CatalogDtos.CategoryView::from).toList();
    }

    @Transactional
    public CatalogDtos.CategoryView saveCategory(Long id, CatalogDtos.CategorySaveRequest request) {
        Category category = id == null ? new Category() : categories.findById(id)
                .filter(value -> !value.isDeleted()).orElseThrow(() -> BusinessException.notFound("分类不存在"));
        if (request.parentId() != null && request.parentId().equals(id)) {
            throw new BusinessException("VALIDATION_ERROR", "分类不能以自身作为父分类", HttpStatus.BAD_REQUEST);
        }
        category.setParentId(request.parentId());
        category.setLevel(request.parentId() == null ? 1 : 2);
        category.setName(request.name().trim());
        category.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        category.setStatus(request.status());
        return CatalogDtos.CategoryView.from(categories.save(category));
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categories.findById(id).filter(value -> !value.isDeleted())
                .orElseThrow(() -> BusinessException.notFound("分类不存在"));
        if (categories.existsByParentIdAndDeletedFalse(id) || products.existsByCategoryIdAndDeletedFalse(id)) {
            throw BusinessException.conflict("RESOURCE_CONFLICT", "分类仍有关联子分类或商品，不能删除");
        }
        category.setDeleted(true);
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.BrandView> adminBrands() {
        return brands.findByDeletedFalseOrderBySortOrderAsc().stream().map(CatalogDtos.BrandView::from).toList();
    }

    @Transactional
    public CatalogDtos.BrandView saveBrand(Long id, CatalogDtos.BrandSaveRequest request) {
        Brand brand = id == null ? new Brand() : brands.findById(id).filter(value -> !value.isDeleted())
                .orElseThrow(() -> BusinessException.notFound("品牌不存在"));
        brand.setName(request.name().trim());
        brand.setLogoUrl(request.logoUrl());
        brand.setDescription(request.description());
        brand.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        brand.setStatus(request.status());
        return CatalogDtos.BrandView.from(brands.save(brand));
    }

    @Transactional
    public void deleteBrand(Long id) {
        Brand brand = brands.findById(id).filter(value -> !value.isDeleted())
                .orElseThrow(() -> BusinessException.notFound("品牌不存在"));
        if (products.existsByBrandIdAndDeletedFalse(id)) {
            throw BusinessException.conflict("RESOURCE_CONFLICT", "品牌仍有关联商品，不能删除");
        }
        brand.setDeleted(true);
    }

    private void apply(Product product, CatalogDtos.ProductSaveRequest request) {
        Category category = categories.findById(request.categoryId()).filter(value -> !value.isDeleted())
                .orElseThrow(() -> BusinessException.notFound("分类不存在"));
        Brand brand = request.brandId() == null ? null : brands.findById(request.brandId())
                .filter(value -> !value.isDeleted()).orElseThrow(() -> BusinessException.notFound("品牌不存在"));
        product.setProductNo(request.productNo().trim());
        product.setName(request.name().trim());
        product.setSubtitle(request.subtitle());
        product.setCategory(category);
        product.setBrand(brand);
        product.setSalePrice(request.salePrice());
        product.setOriginalPrice(request.originalPrice());
        product.setStock(request.stock());
        product.setMainImageUrl(request.mainImageUrl());
        product.setImageUrlsJson(blankJson(request.imageUrlsJson(), "[]"));
        product.setDescription(request.description());
        product.setKeywords(request.keywords());
        product.setScenarios(request.scenarios());
        product.setAudiences(request.audiences());
        product.setSpecificationJson(blankJson(request.specificationJson(), "{}"));
        product.setStatus(request.status());
        if (request.status() == Enums.ProductStatus.ON_SALE && product.getPublishedAt() == null) {
            validatePublish(product);
            product.setPublishedAt(Instant.now());
        }
    }

    private String blankJson(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private Product requiredProduct(Long id) {
        return products.findByIdAndDeletedFalse(id).orElseThrow(() -> BusinessException.notFound("商品不存在"));
    }
    private void validatePublish(Product product) {
        if (product.getStock() == null || product.getStock() < 0 || product.getSalePrice() == null
                || product.getMainImageUrl() == null || product.getMainImageUrl().isBlank()) {
            throw new BusinessException("VALIDATION_ERROR", "商品信息不完整，不能上架", HttpStatus.BAD_REQUEST);
        }
        if (product.getCategory().getStatus() != Enums.CommonStatus.ENABLED
                || (product.getBrand() != null && product.getBrand().getStatus() != Enums.CommonStatus.ENABLED)) {
            throw BusinessException.conflict("RESOURCE_CONFLICT", "分类或品牌已被禁用，不能上架");
        }
    }
}
