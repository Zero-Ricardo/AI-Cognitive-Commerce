package com.aishop.commerce.hotproduct;

import com.aishop.commerce.catalog.CatalogDtos;
import com.aishop.commerce.catalog.HotProductService;
import com.aishop.commerce.catalog.MysqlHotProductService;
import com.aishop.commerce.domain.Enums;
import com.aishop.commerce.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Primary
@Service
public class RedisHotProductService implements HotProductService {
    private static final Logger log = LoggerFactory.getLogger(RedisHotProductService.class);
    private final StringRedisTemplate redis;
    private final ProductRepository products;
    private final JdbcTemplate jdbc;
    private final MysqlHotProductService legacyFallback;
    private final boolean redisEnabled;

    public RedisHotProductService(StringRedisTemplate redis, ProductRepository products, JdbcTemplate jdbc,
                                  MysqlHotProductService legacyFallback,
                                  @Value("${app.redis.hot-products-enabled:false}") boolean redisEnabled) {
        this.redis = redis;
        this.products = products;
        this.jdbc = jdbc;
        this.legacyFallback = legacyFallback;
        this.redisEnabled = redisEnabled;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogDtos.ProductView> getHotProducts(Long categoryId, int limit) {
        List<Long> ids = redisEnabled ? redisIds(categoryId, limit * 3) : List.of();
        if (ids.isEmpty()) ids = snapshotIds(categoryId, limit * 3);
        if (ids.isEmpty()) return legacyFallback.getHotProducts(categoryId, limit);
        Map<Long, com.aishop.commerce.domain.Product> byId = products.findAllById(ids).stream()
                .filter(value -> !value.isDeleted() && value.getStatus() == Enums.ProductStatus.ON_SALE && value.getStock() > 0)
                .collect(java.util.stream.Collectors.toMap(com.aishop.commerce.domain.Product::getId, value -> value));
        return ids.stream().map(byId::get).filter(Objects::nonNull).limit(limit).map(CatalogDtos.ProductView::from).toList();
    }

    private List<Long> redisIds(Long categoryId, int limit) {
        try {
            String key = categoryId == null ? HotProductRefreshService.globalKey() : HotProductRefreshService.categoryKey(categoryId);
            Set<String> values = redis.opsForZSet().reverseRange(key, 0, Math.max(0, limit - 1));
            return values == null ? List.of() : values.stream().map(Long::valueOf).toList();
        } catch (Exception ex) {
            log.warn("读取 Redis 热销榜失败，降级到 MySQL: {}", ex.getMessage());
            return List.of();
        }
    }

    private List<Long> snapshotIds(Long categoryId, int limit) {
        String filter = categoryId == null ? "" : " AND (p.category_id = ? OR c.parent_id = ?)";
        String sql = """
            SELECT h.product_id FROM product_hot_snapshot h
            JOIN products p ON p.id = h.product_id
            JOIN categories c ON c.id = p.category_id
            WHERE p.deleted = FALSE AND p.status = 'ON_SALE' AND p.stock > 0
            """ + filter + " ORDER BY h.hot_score DESC, h.sales_30d DESC, p.published_at DESC, p.id DESC LIMIT ?";
        if (categoryId == null) return jdbc.query(sql, (rs, rowNum) -> rs.getLong(1), limit);
        return jdbc.query(sql, (rs, rowNum) -> rs.getLong(1), categoryId, categoryId, limit);
    }
}
