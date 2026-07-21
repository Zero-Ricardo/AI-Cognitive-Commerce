package com.aishop.commerce.search;

import com.aishop.commerce.domain.Product;
import com.aishop.commerce.domain.ProductSearchOutbox;
import com.aishop.commerce.repository.ProductSearchOutboxRepository;
import org.springframework.stereotype.Service;

@Service
public class ProductIndexOutboxService {
    private final ProductSearchOutboxRepository outbox;
    public ProductIndexOutboxService(ProductSearchOutboxRepository outbox) { this.outbox = outbox; }

    public void enqueue(Product product, ProductSearchOutbox.EventType eventType) {
        var event = new ProductSearchOutbox();
        event.setProductId(product.getId());
        event.setProductVersion(product.getVersion() == null ? 0L : product.getVersion().longValue());
        event.setEventType(eventType);
        outbox.save(event);
    }
}
