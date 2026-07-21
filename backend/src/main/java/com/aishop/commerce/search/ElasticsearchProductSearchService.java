package com.aishop.commerce.search;

import com.aishop.commerce.catalog.CatalogDtos;
import com.aishop.commerce.catalog.MysqlProductSearchService;
import com.aishop.commerce.common.BusinessException;
import com.aishop.commerce.domain.Product;
import com.aishop.commerce.repository.BrandRepository;
import com.aishop.commerce.repository.CategoryRepository;
import com.aishop.commerce.repository.ProductRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class ElasticsearchProductSearchService {
    private static final Logger log = LoggerFactory.getLogger(ElasticsearchProductSearchService.class);
    private final ElasticsearchHttpClient elasticsearch;
    private final MysqlProductSearchService mysqlFallback;
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final BrandRepository brands;
    private final ObjectMapper mapper;
    private final boolean fallbackEnabled;

    public ElasticsearchProductSearchService(ElasticsearchHttpClient elasticsearch,
            MysqlProductSearchService mysqlFallback, ProductRepository products, CategoryRepository categories,
            BrandRepository brands, ObjectMapper mapper,
            @Value("${app.search.mysql-fallback-enabled:true}") boolean fallbackEnabled) {
        this.elasticsearch = elasticsearch;
        this.mysqlFallback = mysqlFallback;
        this.products = products;
        this.categories = categories;
        this.brands = brands;
        this.mapper = mapper;
        this.fallbackEnabled = fallbackEnabled;
    }

    @Transactional(readOnly = true)
    public SearchResponse search(CatalogDtos.ProductSearchCondition condition) {
        validate(condition);
        if (!elasticsearch.enabled()) return fallback(condition);
        try {
            JsonNode response = elasticsearch.post("/" + elasticsearch.indexName() + "/_search", searchBody(condition));
            List<Long> ids = new ArrayList<>();
            response.path("hits").path("hits").forEach(hit -> ids.add(hit.path("_source").path("productId").asLong()));
            Map<Long, Product> productMap = products.findAllById(ids).stream()
                    .filter(value -> !value.isDeleted() && value.getStatus() == com.aishop.commerce.domain.Enums.ProductStatus.ON_SALE)
                    .collect(java.util.stream.Collectors.toMap(Product::getId, value -> value));
            List<CatalogDtos.ProductView> items = ids.stream().map(productMap::get).filter(Objects::nonNull)
                    .map(CatalogDtos.ProductView::from).toList();
            long total = response.path("hits").path("total").path("value").asLong(items.size());
            return new SearchResponse(items, total, condition.page(), condition.pageSize(), response.path("took").asLong(),
                    false, facets(response));
        } catch (Exception ex) {
            log.warn("Elasticsearch 搜索失败，准备降级: {}", ex.getMessage());
            if (fallbackEnabled) return fallback(condition);
            throw new BusinessException("SEARCH_SERVICE_UNAVAILABLE", "搜索服务暂时不可用", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    public List<Suggestion> suggestions(String keyword, int limit) {
        if (keyword == null || keyword.trim().length() < 1 || !elasticsearch.enabled()) return List.of();
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("size", Math.min(limit * 2, 20));
            body.set("_source", mapper.valueToTree(List.of("productId", "name", "categoryId", "categoryName", "brandId", "brandName")));
            ObjectNode bool = body.putObject("query").putObject("bool");
            bool.putArray("must").addObject().putObject("multi_match")
                    .put("query", keyword.trim()).put("type", "bool_prefix")
                    .set("fields", mapper.valueToTree(List.of("name^3", "keywords^2", "brandName")));
            bool.putArray("filter").addObject().putObject("term").put("status", "ON_SALE");
            JsonNode result = elasticsearch.post("/" + elasticsearch.indexName() + "/_search", body);
            LinkedHashMap<String, Suggestion> values = new LinkedHashMap<>();
            result.path("hits").path("hits").forEach(hit -> {
                JsonNode source = hit.path("_source");
                addSuggestion(values, new Suggestion("PRODUCT", source.path("name").asText(), source.path("productId").asText()), limit);
                if (!source.path("categoryName").isMissingNode()) addSuggestion(values,
                        new Suggestion("CATEGORY", source.path("categoryName").asText(), source.path("categoryId").asText()), limit);
                if (!source.path("brandName").isMissingNode() && !source.path("brandName").isNull()) addSuggestion(values,
                        new Suggestion("BRAND", source.path("brandName").asText(), source.path("brandId").asText()), limit);
            });
            return values.values().stream().limit(limit).toList();
        } catch (Exception ex) {
            log.debug("搜索建议不可用: {}", ex.getMessage());
            return List.of();
        }
    }

    private ObjectNode searchBody(CatalogDtos.ProductSearchCondition condition) {
        ObjectNode body = mapper.createObjectNode();
        body.put("from", (condition.page() - 1) * condition.pageSize());
        body.put("size", condition.pageSize());
        body.put("track_total_hits", true);
        ObjectNode bool = body.putObject("query").putObject("bool");
        ArrayNode must = bool.putArray("must");
        if (condition.keyword() == null || condition.keyword().isBlank()) must.addObject().putObject("match_all");
        else {
            ObjectNode match = must.addObject().putObject("multi_match");
            match.put("query", condition.keyword().trim());
            match.set("fields", mapper.valueToTree(List.of("name^5", "keywords^3", "subtitle^2", "brandName^2", "description")));
            match.put("operator", "and");
        }
        ArrayNode filters = bool.putArray("filter");
        filters.addObject().putObject("term").put("status", "ON_SALE");
        filters.addObject().putObject("term").put("deleted", false);
        if (condition.categoryId() != null) {
            ArrayNode should = filters.addObject().putObject("bool").putArray("should");
            should.addObject().putObject("term").put("categoryId", condition.categoryId());
            should.addObject().putObject("term").put("parentCategoryId", condition.categoryId());
        }
        if (condition.brandId() != null) filters.addObject().putObject("term").put("brandId", condition.brandId());
        if (condition.priceMin() != null || condition.priceMax() != null) {
            ObjectNode range = filters.addObject().putObject("range").putObject("salePrice");
            if (condition.priceMin() != null) range.put("gte", condition.priceMin());
            if (condition.priceMax() != null) range.put("lte", condition.priceMax());
        }
        if (Boolean.TRUE.equals(condition.inStock())) filters.addObject().putObject("range").putObject("stock").put("gt", 0);
        ArrayNode sort = body.putArray("sort");
        switch (condition.sort() == null ? CatalogDtos.ProductSort.RELEVANCE : condition.sort()) {
            case PRICE_ASC -> { sort.addObject().putObject("salePrice").put("order", "asc"); sort.addObject().putObject("productId").put("order", "desc"); }
            case PRICE_DESC -> { sort.addObject().putObject("salePrice").put("order", "desc"); sort.addObject().putObject("productId").put("order", "desc"); }
            case NEWEST -> { sort.addObject().putObject("publishedAt").put("order", "desc"); sort.addObject().putObject("productId").put("order", "desc"); }
            case HOT, POPULARITY -> { sort.addObject().putObject("hotScore").put("order", "desc"); sort.addObject().putObject("productId").put("order", "desc"); }
            case RELEVANCE -> { sort.addObject().putObject("_score").put("order", "desc"); sort.addObject().putObject("productId").put("order", "desc"); }
        }
        body.putObject("aggs").putObject("categories").putObject("terms").put("field", "categoryId").put("size", 100);
        body.withObject("aggs").putObject("brands").putObject("terms").put("field", "brandId").put("size", 100);
        return body;
    }

    private Facets facets(JsonNode response) {
        List<FacetItem> categoryItems = new ArrayList<>();
        response.path("aggregations").path("categories").path("buckets").forEach(bucket -> {
            long id = bucket.path("key").asLong();
            categories.findById(id).ifPresent(value -> categoryItems.add(new FacetItem(id, value.getName(), bucket.path("doc_count").asLong())));
        });
        List<FacetItem> brandItems = new ArrayList<>();
        response.path("aggregations").path("brands").path("buckets").forEach(bucket -> {
            long id = bucket.path("key").asLong();
            brands.findById(id).ifPresent(value -> brandItems.add(new FacetItem(id, value.getName(), bucket.path("doc_count").asLong())));
        });
        return new Facets(categoryItems, brandItems);
    }

    private SearchResponse fallback(CatalogDtos.ProductSearchCondition condition) {
        var page = mysqlFallback.search(condition, false);
        return new SearchResponse(page.items(), page.total(), page.page(), page.pageSize(), 0, true,
                new Facets(List.of(), List.of()));
    }

    private void validate(CatalogDtos.ProductSearchCondition condition) {
        if (condition.page() < 1 || condition.pageSize() < 1 || condition.pageSize() > 100)
            throw new BusinessException("SEARCH_PARAM_INVALID", "分页参数不正确", HttpStatus.BAD_REQUEST);
        BigDecimal min = condition.priceMin(), max = condition.priceMax();
        if (min != null && min.signum() < 0 || max != null && max.signum() < 0 || min != null && max != null && min.compareTo(max) > 0)
            throw new BusinessException("SEARCH_PARAM_INVALID", "价格区间不正确", HttpStatus.BAD_REQUEST);
    }

    private void addSuggestion(Map<String, Suggestion> values, Suggestion value, int limit) {
        if (values.size() < limit && value.label() != null && !value.label().isBlank()) values.putIfAbsent(value.type() + ":" + value.id(), value);
    }

    public record FacetItem(Long id, String name, long count) {}
    public record Facets(List<FacetItem> categories, List<FacetItem> brands) {}
    public record SearchResponse(List<CatalogDtos.ProductView> items, long total, int page, int pageSize,
                                 long tookMs, boolean degraded, Facets facets) {}
    public record Suggestion(String type, String label, String id) {}
}
