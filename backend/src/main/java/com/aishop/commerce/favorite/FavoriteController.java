package com.aishop.commerce.favorite;

import com.aishop.commerce.common.ApiResponse;
import com.aishop.commerce.common.PageResult;
import com.aishop.commerce.security.AppUserPrincipal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/favorites")
public class FavoriteController {
    private final FavoriteService service;
    public FavoriteController(FavoriteService service) { this.service = service; }

    @GetMapping
    public ApiResponse<PageResult<FavoriteService.FavoriteView>> list(@AuthenticationPrincipal AppUserPrincipal user,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        return ApiResponse.ok(service.list(user.id(), page, pageSize));
    }
    @GetMapping("/{productId}/status")
    public ApiResponse<Map<String, Boolean>> status(@AuthenticationPrincipal AppUserPrincipal user, @PathVariable Long productId) {
        return ApiResponse.ok(Map.of("favorite", service.status(user.id(), productId)));
    }
    @PutMapping("/{productId}")
    public ApiResponse<Void> add(@AuthenticationPrincipal AppUserPrincipal user, @PathVariable Long productId) {
        service.add(user.id(), productId); return ApiResponse.ok(null);
    }
    @DeleteMapping("/{productId}")
    public ApiResponse<Void> remove(@AuthenticationPrincipal AppUserPrincipal user, @PathVariable Long productId) {
        service.remove(user.id(), productId); return ApiResponse.ok(null);
    }
}
