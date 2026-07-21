package com.aishop.commerce.catalog;

import com.aishop.commerce.domain.Enums;
import com.aishop.commerce.repository.ProductEventRepository;
import com.aishop.commerce.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashMap;

@Service
public class MysqlHotProductService implements HotProductService {
    private final ProductEventRepository events;
    private final ProductRepository products;
    public MysqlHotProductService(ProductEventRepository events, ProductRepository products) {
        this.events = events; this.products = products;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogDtos.ProductView> getHotProducts(Long categoryId, int limit) {
        List<Long> ids = events.findHotProductScores(categoryId).stream().map(row -> (Long) row[0]).limit(limit).toList();
        var scoredProducts = products.findAllById(ids).stream()
                .filter(p -> !p.isDeleted() && p.getStatus() == Enums.ProductStatus.ON_SALE)
                .collect(java.util.stream.Collectors.toMap(com.aishop.commerce.domain.Product::getId, p -> p));

        // Keep behavior-ranked products first, then fill sparse rankings with newly published products.
        // This prevents a single click/cart event from shrinking the whole homepage recommendation area.
        var ranked = new LinkedHashMap<Long, com.aishop.commerce.domain.Product>();
        ids.forEach(id -> {
            var product = scoredProducts.get(id);
            if (product != null) ranked.put(id, product);
        });
        products.findTop20ByDeletedFalseAndStatusOrderByPublishedAtDesc(Enums.ProductStatus.ON_SALE).stream()
                .filter(p -> categoryId == null || p.getCategory().getId().equals(categoryId)
                        || categoryId.equals(p.getCategory().getParentId()))
                .forEach(p -> ranked.putIfAbsent(p.getId(), p));

        return ranked.values().stream().limit(limit).map(CatalogDtos.ProductView::from).toList();
    }
}
