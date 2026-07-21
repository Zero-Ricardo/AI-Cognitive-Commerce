package com.aishop.commerce.hotproduct;

import com.aishop.commerce.catalog.CatalogDtos;
import com.aishop.commerce.catalog.HotProductService;
import com.aishop.commerce.common.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/v2/products")
public class HotProductV2Controller {
    private final HotProductService service;
    public HotProductV2Controller(HotProductService service) { this.service = service; }

    @GetMapping("/hot")
    public ApiResponse<HotResponse> hot(@RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "8") @Min(1) @Max(50) int limit) {
        AtomicInteger rank = new AtomicInteger();
        List<HotItem> items = service.getHotProducts(categoryId, limit).stream()
                .map(product -> new HotItem(rank.incrementAndGet(), "热销", product)).toList();
        return ApiResponse.ok(new HotResponse(items, Instant.now(), false));
    }

    public record HotItem(int rank, String hotLabel, CatalogDtos.ProductView product) {}
    public record HotResponse(List<HotItem> items, Instant calculatedAt, boolean degraded) {}
}
