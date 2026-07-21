package com.aishop.commerce.hotproduct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class HotProductRefreshService {
    private static final Logger log = LoggerFactory.getLogger(HotProductRefreshService.class);
    private static final String GLOBAL_KEY = "commerce:hot:products:global:v2";
    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;
    private final boolean redisEnabled;
    private final double salesWeight;
    private final double viewWeight;
    private final double favoriteWeight;
    private final String formulaVersion;

    public HotProductRefreshService(JdbcTemplate jdbc, StringRedisTemplate redis,
            @Value("${app.redis.hot-products-enabled:false}") boolean redisEnabled,
            @Value("${app.hot-products.sales-weight:0.50}") double salesWeight,
            @Value("${app.hot-products.view-weight:0.30}") double viewWeight,
            @Value("${app.hot-products.favorite-weight:0.20}") double favoriteWeight,
            @Value("${app.hot-products.formula-version:hot-v2-log-1}") String formulaVersion) {
        this.jdbc = jdbc;
        this.redis = redis;
        this.redisEnabled = redisEnabled;
        this.salesWeight = salesWeight;
        this.viewWeight = viewWeight;
        this.favoriteWeight = favoriteWeight;
        this.formulaVersion = formulaVersion;
        if (Math.abs(salesWeight + viewWeight + favoriteWeight - 1D) > 0.000001D) {
            throw new IllegalArgumentException("热销商品三项权重之和必须为 1");
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() { refreshSafely(); }

    @Scheduled(fixedDelayString = "${app.hot-products.refresh-ms:300000}", initialDelayString = "${app.hot-products.refresh-ms:300000}")
    public void scheduledRefresh() { refreshSafely(); }

    private void refreshSafely() {
        try { refresh(); }
        catch (Exception ex) { log.warn("刷新热销商品失败，将继续使用上一份快照: {}", ex.getMessage()); }
    }

    @Transactional
    public void refresh() {
        String sql = """
            SELECT p.id product_id, p.category_id,
                   COALESCE(c.parent_id, p.category_id) parent_category_id,
                   COALESCE(s.sales_30d, 0) sales_30d,
                   COALESCE(v.views_7d, 0) views_7d,
                   COALESCE(f.favorites_active, 0) favorites_active
            FROM products p
            JOIN categories c ON c.id = p.category_id
            LEFT JOIN (
                SELECT product_id, SUM(sales_quantity) sales_30d
                FROM product_sales_daily
                WHERE metric_date >= CURRENT_DATE - INTERVAL 29 DAY
                GROUP BY product_id
            ) s ON s.product_id = p.id
            LEFT JOIN (
                SELECT product_id, COUNT(*) views_7d
                FROM product_events
                WHERE event_type = 'VIEW' AND occurred_at >= CURRENT_TIMESTAMP - INTERVAL 7 DAY
                GROUP BY product_id
            ) v ON v.product_id = p.id
            LEFT JOIN (
                SELECT product_id, COUNT(*) favorites_active
                FROM favorites GROUP BY product_id
            ) f ON f.product_id = p.id
            WHERE p.deleted = FALSE AND p.status = 'ON_SALE' AND p.stock > 0
            """;
        List<Metric> metrics = jdbc.query(sql, (rs, rowNum) -> new Metric(
                rs.getLong("product_id"), rs.getLong("category_id"), rs.getLong("parent_category_id"),
                rs.getLong("sales_30d"), rs.getLong("views_7d"), rs.getLong("favorites_active")));
        Instant now = Instant.now();
        jdbc.batchUpdate("""
            INSERT INTO product_hot_snapshot
                (product_id, sales_30d, views_7d, favorites_active, hot_score, calculated_at, formula_version)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE sales_30d=VALUES(sales_30d), views_7d=VALUES(views_7d),
                favorites_active=VALUES(favorites_active), hot_score=VALUES(hot_score),
                calculated_at=VALUES(calculated_at), formula_version=VALUES(formula_version)
            """, metrics, 100, (ps, metric) -> {
                ps.setLong(1, metric.productId());
                ps.setLong(2, metric.sales30d());
                ps.setLong(3, metric.views7d());
                ps.setLong(4, metric.favoritesActive());
                ps.setDouble(5, score(metric));
                ps.setObject(6, now);
                ps.setString(7, formulaVersion);
            });
        if (redisEnabled) publishRedis(metrics);
    }

    private double score(Metric metric) {
        return salesWeight * Math.log1p(metric.sales30d())
                + viewWeight * Math.log1p(metric.views7d())
                + favoriteWeight * Math.log1p(metric.favoritesActive());
    }

    private void publishRedis(List<Metric> metrics) {
        String suffix = ":tmp:" + UUID.randomUUID();
        Map<String, Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>>> values = new HashMap<>();
        for (Metric metric : metrics) {
            double score = score(metric);
            values.computeIfAbsent(GLOBAL_KEY + suffix, key -> new HashSet<>())
                    .add(org.springframework.data.redis.core.ZSetOperations.TypedTuple.of(metric.productId().toString(), score));
            Set<Long> categoryIds = new HashSet<>();
            categoryIds.add(metric.categoryId());
            categoryIds.add(metric.parentCategoryId());
            for (Long categoryId : categoryIds) {
                values.computeIfAbsent(categoryKey(categoryId) + suffix, key -> new HashSet<>())
                        .add(org.springframework.data.redis.core.ZSetOperations.TypedTuple.of(metric.productId().toString(), score));
            }
        }
        values.forEach((key, tuples) -> redis.opsForZSet().add(key, tuples));
        values.keySet().forEach(tempKey -> {
            String finalKey = tempKey.substring(0, tempKey.indexOf(":tmp:"));
            redis.rename(tempKey, finalKey);
        });
    }

    public static String globalKey() { return GLOBAL_KEY; }
    public static String categoryKey(Long categoryId) { return "commerce:hot:products:category:" + categoryId + ":v2"; }

    record Metric(Long productId, Long categoryId, Long parentCategoryId, long sales30d, long views7d, long favoritesActive) {}
}
