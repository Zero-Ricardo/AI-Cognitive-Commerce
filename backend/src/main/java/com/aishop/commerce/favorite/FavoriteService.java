package com.aishop.commerce.favorite;

import com.aishop.commerce.catalog.CatalogDtos;
import com.aishop.commerce.common.BusinessException;
import com.aishop.commerce.common.PageResult;
import com.aishop.commerce.domain.Enums;
import com.aishop.commerce.domain.Favorite;
import com.aishop.commerce.domain.ProductEvent;
import com.aishop.commerce.repository.FavoriteRepository;
import com.aishop.commerce.repository.ProductEventRepository;
import com.aishop.commerce.repository.ProductRepository;
import com.aishop.commerce.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FavoriteService {
    private final FavoriteRepository favorites;
    private final ProductRepository products;
    private final UserRepository users;
    private final ProductEventRepository events;

    public FavoriteService(FavoriteRepository favorites, ProductRepository products, UserRepository users,
                           ProductEventRepository events) {
        this.favorites = favorites; this.products = products; this.users = users; this.events = events;
    }

    @Transactional(readOnly = true)
    public PageResult<FavoriteView> list(Long userId, int page, int pageSize) {
        var result = favorites.findByUserId(userId, PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(value -> new FavoriteView(value.getId(), value.getCreatedAt(), CatalogDtos.ProductView.from(value.getProduct())));
        return PageResult.from(result);
    }

    @Transactional
    public void add(Long userId, Long productId) {
        if (favorites.existsByUserIdAndProductId(userId, productId)) return;
        var product = products.findByIdAndDeletedFalseAndStatus(productId, Enums.ProductStatus.ON_SALE)
                .orElseThrow(() -> BusinessException.notFound("商品不存在或已下架"));
        var favorite = new Favorite();
        favorite.setUser(users.getReferenceById(userId));
        favorite.setProduct(product);
        favorites.save(favorite);
        var event = new ProductEvent();
        event.setUser(users.getReferenceById(userId));
        event.setProduct(product);
        event.setEventType(Enums.ProductEventType.FAVORITE);
        event.setScore(3);
        events.save(event);
    }

    @Transactional
    public void remove(Long userId, Long productId) {
        favorites.findByUserIdAndProductId(userId, productId).ifPresent(favorites::delete);
    }

    @Transactional(readOnly = true)
    public boolean status(Long userId, Long productId) { return favorites.existsByUserIdAndProductId(userId, productId); }

    public record FavoriteView(Long id, java.time.Instant createdAt, CatalogDtos.ProductView product) {}
}
