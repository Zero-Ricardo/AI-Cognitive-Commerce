package com.aishop.commerce.cart;

import com.aishop.commerce.common.ApiResponse;
import com.aishop.commerce.security.AppUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {
    private final CartService service;
    public CartController(CartService service) { this.service = service; }

    @GetMapping
    public ApiResponse<CartService.CartView> get(@AuthenticationPrincipal AppUserPrincipal user) {
        return ApiResponse.ok(service.get(user.id()));
    }
    @PostMapping("/items")
    public ApiResponse<CartService.CartView> add(@AuthenticationPrincipal AppUserPrincipal user,
            @Valid @RequestBody AddRequest request) {
        return ApiResponse.ok(service.add(user.id(), request.productId(), request.quantity()));
    }
    @PatchMapping("/items/{itemId}")
    public ApiResponse<CartService.CartView> update(@AuthenticationPrincipal AppUserPrincipal user,
            @PathVariable Long itemId, @Valid @RequestBody UpdateRequest request) {
        return ApiResponse.ok(service.update(user.id(), itemId, request.quantity(), request.selected()));
    }
    @DeleteMapping("/items/{itemId}")
    public ApiResponse<CartService.CartView> remove(@AuthenticationPrincipal AppUserPrincipal user, @PathVariable Long itemId) {
        return ApiResponse.ok(service.remove(user.id(), itemId));
    }
    @PatchMapping("/items/selection")
    public ApiResponse<CartService.CartView> selection(@AuthenticationPrincipal AppUserPrincipal user,
            @Valid @RequestBody SelectionRequest request) {
        return ApiResponse.ok(service.selectAll(user.id(), request.selected()));
    }

    public record AddRequest(@NotNull Long productId, @Min(1) int quantity) {}
    public record UpdateRequest(@Min(1) Integer quantity, Boolean selected) {}
    public record SelectionRequest(boolean selected) {}
}
