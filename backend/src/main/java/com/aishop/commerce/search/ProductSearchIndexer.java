package com.aishop.commerce.search;

import com.aishop.commerce.domain.Product;
import com.aishop.commerce.domain.ProductSearchOutbox;
import com.aishop.commerce.repository.ProductRepository;
import com.aishop.commerce.repository.ProductSearchOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class ProductSearchIndexer {
    private static final Logger log = LoggerFactory.getLogger(ProductSearchIndexer.class);
    private final ElasticsearchHttpClient elasticsearch;
    private final ProductSearchOutboxRepository outbox;
    private final ProductRepository products;
    private final ObjectMapper mapper;

    public ProductSearchIndexer(ElasticsearchHttpClient elasticsearch, ProductSearchOutboxRepository outbox,
                                ProductRepository products, ObjectMapper mapper) {
        this.elasticsearch = elasticsearch;
        this.outbox = outbox;
        this.products = products;
        this.mapper = mapper;
    }

    @Scheduled(fixedDelayString = "${app.search.index-refresh-ms:5000}", initialDelay = 3000)
    public void sync() {
        if (!elasticsearch.enabled()) return;
        try {
            ensureIndex();
            List<ProductSearchOutbox> events = new ArrayList<>(outbox.findByStatusAndNextRetryAtIsNullOrderByIdAsc(
                    ProductSearchOutbox.Status.NEW, PageRequest.of(0, 50)));
            if (events.size() < 50) events.addAll(outbox.findByStatusAndNextRetryAtBeforeOrderByIdAsc(
                    ProductSearchOutbox.Status.NEW, Instant.now(), PageRequest.of(0, 50 - events.size())));
            for (ProductSearchOutbox event : events) process(event);
        } catch (Exception ex) {
            log.warn("同步商品索引失败，稍后重试: {}", ex.getMessage());
        }
    }

    private void ensureIndex() throws Exception {
        try { elasticsearch.get("/" + elasticsearch.indexName()); return; }
        catch (Exception ignored) { }
        ObjectNode root = mapper.createObjectNode();
        root.set("settings", mapper.readTree("""
            {"index":{"max_ngram_diff":2},"analysis":{"tokenizer":{"commerce_ngram":{"type":"ngram","min_gram":1,"max_gram":3,"token_chars":["letter","digit"]}},
             "analyzer":{"commerce_index":{"type":"custom","tokenizer":"commerce_ngram","filter":["lowercase"]},
                         "commerce_search":{"type":"custom","tokenizer":"standard","filter":["lowercase"]}}}}
            """));
        root.set("mappings", mapper.readTree("""
            {"dynamic":"strict","properties":{
              "productId":{"type":"long"},"productNo":{"type":"keyword"},
              "name":{"type":"text","analyzer":"commerce_index","search_analyzer":"commerce_search","fields":{"raw":{"type":"keyword"}}},
              "subtitle":{"type":"text","analyzer":"commerce_index","search_analyzer":"commerce_search"},
              "description":{"type":"text","analyzer":"commerce_index","search_analyzer":"commerce_search"},
              "keywords":{"type":"text","analyzer":"commerce_index","search_analyzer":"commerce_search"},
              "categoryId":{"type":"long"},"parentCategoryId":{"type":"long"},"categoryName":{"type":"keyword"},
              "brandId":{"type":"long"},"brandName":{"type":"keyword"},
              "salePrice":{"type":"scaled_float","scaling_factor":100},"stock":{"type":"integer"},
              "status":{"type":"keyword"},"deleted":{"type":"boolean"},"publishedAt":{"type":"date"},
              "hotScore":{"type":"double"},"version":{"type":"long"},"updatedAt":{"type":"date"}
            }}
            """));
        try { elasticsearch.put("/" + elasticsearch.indexName(), root); }
        catch (Exception ex) {
            // Another instance may have created it concurrently.
            elasticsearch.get("/" + elasticsearch.indexName());
        }
    }

    private void process(ProductSearchOutbox event) {
        try {
            if (event.getEventType() == ProductSearchOutbox.EventType.DELETE) {
                try { elasticsearch.delete("/" + elasticsearch.indexName() + "/_doc/" + event.getProductId()); }
                catch (Exception ignored) { }
            } else {
                Optional<Product> product = products.findWithCategoryAndBrandById(event.getProductId());
                if (product.isEmpty() || product.get().isDeleted()) {
                    try { elasticsearch.delete("/" + elasticsearch.indexName() + "/_doc/" + event.getProductId()); }
                    catch (Exception ignored) { }
                } else {
                    elasticsearch.put("/" + elasticsearch.indexName() + "/_doc/" + event.getProductId(), document(product.get()));
                }
            }
            event.setStatus(ProductSearchOutbox.Status.DONE);
            event.setProcessedAt(Instant.now());
            event.setLastError(null);
        } catch (Exception ex) {
            event.setRetryCount(event.getRetryCount() + 1);
            event.setLastError(abbreviate(ex.getMessage()));
            if (event.getRetryCount() >= 10) event.setStatus(ProductSearchOutbox.Status.FAILED);
            else event.setNextRetryAt(Instant.now().plus(Math.min(300, 1L << Math.min(8, event.getRetryCount())), ChronoUnit.SECONDS));
        }
        outbox.save(event);
    }

    private ObjectNode document(Product product) {
        ObjectNode node = mapper.createObjectNode();
        node.put("productId", product.getId());
        node.put("productNo", product.getProductNo());
        node.put("name", product.getName());
        putNullable(node, "subtitle", product.getSubtitle());
        node.put("description", product.getDescription());
        putNullable(node, "keywords", product.getKeywords());
        node.put("categoryId", product.getCategory().getId());
        node.put("parentCategoryId", product.getCategory().getParentId() == null ? product.getCategory().getId() : product.getCategory().getParentId());
        node.put("categoryName", product.getCategory().getName());
        if (product.getBrand() == null) { node.putNull("brandId"); node.putNull("brandName"); }
        else { node.put("brandId", product.getBrand().getId()); node.put("brandName", product.getBrand().getName()); }
        node.put("salePrice", product.getSalePrice());
        node.put("stock", product.getStock());
        node.put("status", product.getStatus().name());
        node.put("deleted", product.isDeleted());
        if (product.getPublishedAt() != null) node.put("publishedAt", product.getPublishedAt().toString()); else node.putNull("publishedAt");
        node.put("hotScore", 0D);
        node.put("version", product.getVersion() == null ? 0 : product.getVersion());
        node.put("updatedAt", product.getUpdatedAt().toString());
        return node;
    }

    private void putNullable(ObjectNode node, String field, String value) { if (value == null) node.putNull(field); else node.put(field, value); }
    private String abbreviate(String value) { if (value == null) return null; return value.length() > 500 ? value.substring(0, 500) : value; }
}
