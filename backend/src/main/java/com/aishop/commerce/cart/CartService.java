package com.aishop.commerce.cart;

import com.aishop.commerce.catalog.CatalogDtos;
import com.aishop.commerce.common.BusinessException;
import com.aishop.commerce.domain.CartItem;
import com.aishop.commerce.domain.Enums;
import com.aishop.commerce.domain.ProductEvent;
import com.aishop.commerce.repository.CartItemRepository;
import com.aishop.commerce.repository.ProductEventRepository;
import com.aishop.commerce.repository.ProductRepository;
import com.aishop.commerce.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {
    private final CartItemRepository cart;
    private final ProductRepository products;
    private final UserRepository users;
    private final ProductEventRepository events;

    public CartService(CartItemRepository cart, ProductRepository products, UserRepository users,
                       ProductEventRepository events) {
        this.cart = cart; this.products = products; this.users = users; this.events = events;
    }

    @Transactional(readOnly = true)
    public CartView get(Long userId) {
        List<CartItemView> items = cart.findByUserIdOrderByUpdatedAtDesc(userId).stream().map(this::view).toList();
        int selectedCount = items.stream().filter(CartItemView::selected).mapToInt(CartItemView::quantity).sum();
        BigDecimal total = items.stream().filter(item -> item.selected() && item.available())
                .map(CartItemView::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartView(items, items.size(), selectedCount, total);
    }

    @Transactional
    public CartView add(Long userId, Long productId, int quantity) {
        if (quantity < 1) throw validation("数量必须大于0");
        var product = products.findByIdAndDeletedFalseAndStatus(productId, Enums.ProductStatus.ON_SALE)
                .orElseThrow(() -> BusinessException.notFound("商品不存在或已下架"));
        CartItem item = cart.findByUserIdAndProductId(userId, productId).orElseGet(() -> {
            CartItem created = new CartItem();
            created.setUser(users.getReferenceById(userId));
            created.setProduct(product);
            created.setQuantity(0);
            created.setSelected(true);
            return created;
        });
        int target = item.getQuantity() + quantity;
        ensureStock(product.getStock(), target);
        item.setQuantity(target);
        cart.save(item);
        var event = new ProductEvent();
        event.setUser(users.getReferenceById(userId));
        event.setProduct(product);
        event.setEventType(Enums.ProductEventType.ADD_CART);
        event.setScore(5);
        events.save(event);
        return get(userId);
    }

    @Transactional
    public CartView update(Long userId, Long itemId, Integer quantity, Boolean selected) {
        CartItem item = cart.findByIdAndUserId(itemId, userId).orElseThrow(() -> BusinessException.notFound("购物车条目不存在"));
        if (quantity != null) {
            if (quantity < 1) throw validation("数量必须大于0");
            ensureStock(item.getProduct().getStock(), quantity);
            item.setQuantity(quantity);
        }
        if (selected != null) item.setSelected(selected);
        return get(userId);
    }

    @Transactional
    public CartView selectAll(Long userId, boolean selected) {
        cart.findByUserIdOrderByUpdatedAtDesc(userId).forEach(item -> item.setSelected(selected));
        return get(userId);
    }

    @Transactional
    public CartView remove(Long userId, Long itemId) {
        cart.findByIdAndUserId(itemId, userId).ifPresent(cart::delete);
        return get(userId);
    }

    private CartItemView view(CartItem value) {
        var product = value.getProduct();
        boolean available = !product.isDeleted() && product.getStatus() == Enums.ProductStatus.ON_SALE && product.getStock() >= value.getQuantity();
        return new CartItemView(value.getId(), CatalogDtos.ProductView.from(product), value.getQuantity(), value.isSelected(),
                available, product.getSalePrice().multiply(BigDecimal.valueOf(value.getQuantity())));
    }

    private void ensureStock(int stock, int quantity) {
        if (stock < quantity) throw BusinessException.conflict("INSUFFICIENT_STOCK", "库存不足，当前最多可购买 " + stock + " 件");
    }
    private BusinessException validation(String message) { return new BusinessException("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST); }

    public record CartItemView(Long id, CatalogDtos.ProductView product, int quantity, boolean selected,
                               boolean available, BigDecimal subtotal) {}
    public record CartView(List<CartItemView> items, int itemCount, int selectedCount, BigDecimal selectedTotal) {}
}
