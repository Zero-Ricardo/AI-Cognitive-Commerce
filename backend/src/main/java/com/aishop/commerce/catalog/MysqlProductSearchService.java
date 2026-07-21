package com.aishop.commerce.catalog;

import com.aishop.commerce.common.BusinessException;
import com.aishop.commerce.common.PageResult;
import com.aishop.commerce.domain.Enums;
import com.aishop.commerce.domain.Product;
import com.aishop.commerce.repository.ProductRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Locale;

@Service
public class MysqlProductSearchService implements ProductSearchService {
    private final ProductRepository products;

    public MysqlProductSearchService(ProductRepository products) { this.products = products; }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CatalogDtos.ProductView> search(CatalogDtos.ProductSearchCondition c, boolean admin) {
        if (c.page() < 1 || c.pageSize() < 1 || c.pageSize() > 100) {
            throw new BusinessException("VALIDATION_ERROR", "分页参数不正确", HttpStatus.BAD_REQUEST);
        }
        if (c.priceMin() != null && c.priceMax() != null && c.priceMin().compareTo(c.priceMax()) > 0) {
            throw new BusinessException("VALIDATION_ERROR", "最低价格不能高于最高价格", HttpStatus.BAD_REQUEST);
        }
        Specification<Product> specification = (root, query, cb) -> {
            var list = new ArrayList<Predicate>();
            list.add(cb.isFalse(root.get("deleted")));
            if (!admin) list.add(cb.equal(root.get("status"), Enums.ProductStatus.ON_SALE));
            else if (c.status() != null) list.add(cb.equal(root.get("status"), c.status()));
            if (c.categoryId() != null) list.add(cb.or(
                    cb.equal(root.get("category").get("id"), c.categoryId()),
                    cb.equal(root.get("category").get("parentId"), c.categoryId())));
            if (c.brandId() != null) list.add(cb.equal(root.get("brand").get("id"), c.brandId()));
            if (c.priceMin() != null) list.add(cb.greaterThanOrEqualTo(root.get("salePrice"), c.priceMin()));
            if (c.priceMax() != null) list.add(cb.lessThanOrEqualTo(root.get("salePrice"), c.priceMax()));
            if (Boolean.TRUE.equals(c.inStock())) list.add(cb.greaterThan(root.get("stock"), 0));
            if (c.keyword() != null && !c.keyword().isBlank()) {
                String keyword = "%" + c.keyword().trim().toLowerCase(Locale.ROOT) + "%";
                list.add(cb.or(cb.like(cb.lower(root.get("name")), keyword),
                        cb.like(cb.lower(root.get("productNo")), keyword),
                        cb.like(cb.lower(root.get("subtitle")), keyword),
                        cb.like(cb.lower(root.get("keywords")), keyword)));
            }
            return cb.and(list.toArray(Predicate[]::new));
        };
        var page = products.findAll(specification, PageRequest.of(c.page() - 1, c.pageSize(), sort(c.sort())))
                .map(CatalogDtos.ProductView::from);
        return PageResult.from(page);
    }

    private Sort sort(CatalogDtos.ProductSort sort) {
        return switch (sort == null ? CatalogDtos.ProductSort.RELEVANCE : sort) {
            case PRICE_ASC -> Sort.by(Sort.Direction.ASC, "salePrice");
            case PRICE_DESC -> Sort.by(Sort.Direction.DESC, "salePrice");
            case NEWEST, POPULARITY, HOT, RELEVANCE -> Sort.by(Sort.Direction.DESC, "publishedAt").and(Sort.by(Sort.Direction.DESC, "id"));
        };
    }
}
