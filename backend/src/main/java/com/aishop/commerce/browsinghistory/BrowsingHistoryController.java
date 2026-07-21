package com.aishop.commerce.browsinghistory;

import com.aishop.commerce.common.ApiResponse;
import com.aishop.commerce.common.PageResult;
import com.aishop.commerce.security.AppUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v2")
public class BrowsingHistoryController {
    private final BrowsingHistoryService service;
    public BrowsingHistoryController(BrowsingHistoryService service) { this.service = service; }

    @PostMapping("/browsing-history/{productId}")
    public ApiResponse<Void> record(@PathVariable Long productId, @Valid @RequestBody RecordRequest request,
                                    @AuthenticationPrincipal AppUserPrincipal principal,
                                    @RequestHeader(name = "X-Anonymous-Id", required = false) String anonymousId) {
        service.record(principal == null ? null : principal.id(), anonymousId, productId, request.clientViewId());
        return ApiResponse.ok(null);
    }

    @GetMapping("/browsing-history")
    public ApiResponse<PageResult<BrowsingHistoryService.HistoryView>> list(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        return ApiResponse.ok(service.list(principal.id(), page, pageSize));
    }

    @DeleteMapping("/browsing-history/{productId}")
    public ApiResponse<Void> remove(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long productId) {
        service.remove(principal.id(), productId);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/browsing-history")
    public ApiResponse<Void> clear(@AuthenticationPrincipal AppUserPrincipal principal) {
        service.clear(principal.id());
        return ApiResponse.ok(null);
    }

    @PostMapping("/browsing-history:merge")
    public ApiResponse<Void> merge(@AuthenticationPrincipal AppUserPrincipal principal, @Valid @RequestBody MergeRequest request) {
        service.merge(principal.id(), request.items().stream()
                .map(item -> new BrowsingHistoryService.MergeItem(item.productId(), item.lastViewedAt())).toList());
        return ApiResponse.ok(null);
    }

    public record RecordRequest(@NotBlank @Size(max = 64) String clientViewId) {}
    public record MergeItem(Long productId, Instant lastViewedAt) {}
    public record MergeRequest(@NotEmpty @Size(max = 20) List<@Valid MergeItem> items) {}
}
