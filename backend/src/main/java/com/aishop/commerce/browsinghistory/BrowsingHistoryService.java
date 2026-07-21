package com.aishop.commerce.browsinghistory;

import com.aishop.commerce.catalog.CatalogDtos;
import com.aishop.commerce.common.BusinessException;
import com.aishop.commerce.common.PageResult;
import com.aishop.commerce.domain.Enums;
import com.aishop.commerce.domain.ProductEvent;
import com.aishop.commerce.domain.UserBrowsingHistory;
import com.aishop.commerce.repository.ProductEventRepository;
import com.aishop.commerce.repository.ProductRepository;
import com.aishop.commerce.repository.UserBrowsingHistoryRepository;
import com.aishop.commerce.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class BrowsingHistoryService {
    private final UserBrowsingHistoryRepository histories;
    private final ProductEventRepository events;
    private final ProductRepository products;
    private final UserRepository users;
    private final long dedupeMinutes;

    public BrowsingHistoryService(UserBrowsingHistoryRepository histories, ProductEventRepository events,
                                  ProductRepository products, UserRepository users,
                                  @Value("${app.browsing-history.dedupe-minutes:30}") long dedupeMinutes) {
        this.histories = histories;
        this.events = events;
        this.products = products;
        this.users = users;
        this.dedupeMinutes = dedupeMinutes;
    }

    @Transactional
    public void record(Long userId, String anonymousId, Long productId, String clientViewId) {
        var product = products.findByIdAndDeletedFalseAndStatus(productId, Enums.ProductStatus.ON_SALE)
                .orElseThrow(() -> BusinessException.notFound("商品不存在或已下架"));
        Instant now = Instant.now();
        if (userId != null) {
            var history = histories.findByUserIdAndProductId(userId, productId).orElseGet(() -> {
                var created = new UserBrowsingHistory();
                created.setUser(users.getReferenceById(userId));
                created.setProduct(product);
                created.setFirstViewedAt(now);
                created.setViewCount(0);
                return created;
            });
            history.setLastViewedAt(now);
            history.setViewCount(history.getViewCount() + 1);
            histories.save(history);
        }

        if (clientViewId != null && events.existsByClientEventId(clientViewId)) return;
        Instant threshold = now.minus(dedupeMinutes, ChronoUnit.MINUTES);
        boolean duplicate = userId != null
                ? events.existsByUserIdAndProductIdAndEventTypeAndOccurredAtAfter(userId, productId, Enums.ProductEventType.VIEW, threshold)
                : anonymousId != null && events.existsByAnonymousIdAndProductIdAndEventTypeAndOccurredAtAfter(
                        anonymousId, productId, Enums.ProductEventType.VIEW, threshold);
        if (duplicate) return;

        var event = new ProductEvent();
        event.setProduct(product);
        event.setUser(userId == null ? null : users.getReferenceById(userId));
        event.setAnonymousId(normalizeAnonymousId(anonymousId));
        event.setClientEventId(clientViewId);
        event.setEventType(Enums.ProductEventType.VIEW);
        event.setScore(1);
        try {
            events.saveAndFlush(event);
        } catch (DataIntegrityViolationException ignored) {
            // A repeated clientViewId is idempotent.
        }
    }

    @Transactional(readOnly = true)
    public PageResult<HistoryView> list(Long userId, int page, int pageSize) {
        var result = histories.findByUserId(userId,
                PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "lastViewedAt").and(Sort.by(Sort.Direction.DESC, "id"))))
                .map(value -> new HistoryView(value.getId(), CatalogDtos.ProductView.from(value.getProduct()),
                        value.getViewCount(), value.getLastViewedAt()));
        return PageResult.from(result);
    }

    @Transactional
    public void remove(Long userId, Long productId) { histories.deleteByUserIdAndProductId(userId, productId); }

    @Transactional
    public void clear(Long userId) { histories.deleteByUserId(userId); }

    @Transactional
    public void merge(Long userId, List<MergeItem> items) {
        items.stream().limit(20).forEach(item -> {
            var product = products.findByIdAndDeletedFalse(item.productId()).orElse(null);
            if (product == null) return;
            var history = histories.findByUserIdAndProductId(userId, item.productId()).orElseGet(() -> {
                var created = new UserBrowsingHistory();
                created.setUser(users.getReferenceById(userId));
                created.setProduct(product);
                created.setFirstViewedAt(item.lastViewedAt());
                created.setViewCount(1);
                return created;
            });
            if (history.getLastViewedAt() == null || item.lastViewedAt().isAfter(history.getLastViewedAt())) {
                history.setLastViewedAt(item.lastViewedAt());
            }
            histories.save(history);
        });
    }

    private String normalizeAnonymousId(String value) {
        if (value == null || value.isBlank()) return null;
        return value.length() > 64 ? value.substring(0, 64) : value;
    }

    public record HistoryView(Long id, CatalogDtos.ProductView product, Integer viewCount, Instant lastViewedAt) {}
    public record MergeItem(Long productId, Instant lastViewedAt) {}
}
