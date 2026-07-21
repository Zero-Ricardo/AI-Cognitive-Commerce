CREATE TABLE user_browsing_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    view_count INT NOT NULL DEFAULT 1,
    first_viewed_at DATETIME(3) NOT NULL,
    last_viewed_at DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    CONSTRAINT uk_history_user_product UNIQUE (user_id, product_id),
    CONSTRAINT fk_history_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_history_product FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_history_user_last (user_id, last_viewed_at, id)
);

ALTER TABLE product_events
    ADD COLUMN client_event_id VARCHAR(64) NULL,
    ADD CONSTRAINT uk_product_event_client UNIQUE (client_event_id);

CREATE TABLE product_sales_daily (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    metric_date DATE NOT NULL,
    sales_quantity BIGINT NOT NULL DEFAULT 0,
    source VARCHAR(30) NOT NULL,
    source_reference VARCHAR(100) NOT NULL DEFAULT '',
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    CONSTRAINT ck_sales_quantity_non_negative CHECK (sales_quantity >= 0),
    CONSTRAINT uk_sales_source UNIQUE (product_id, metric_date, source, source_reference),
    CONSTRAINT fk_sales_product FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_sales_date_product (metric_date, product_id)
);

CREATE TABLE product_hot_snapshot (
    product_id BIGINT PRIMARY KEY,
    sales_30d BIGINT NOT NULL DEFAULT 0,
    views_7d BIGINT NOT NULL DEFAULT 0,
    favorites_active BIGINT NOT NULL DEFAULT 0,
    hot_score DECIMAL(18,8) NOT NULL DEFAULT 0,
    calculated_at DATETIME(3) NOT NULL,
    formula_version VARCHAR(30) NOT NULL,
    CONSTRAINT fk_hot_snapshot_product FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_hot_snapshot_score (hot_score DESC, product_id DESC)
);

CREATE TABLE product_search_outbox (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    product_version BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME(3) NULL,
    last_error VARCHAR(500) NULL,
    created_at DATETIME(3) NOT NULL,
    processed_at DATETIME(3) NULL,
    INDEX idx_search_outbox_pending (status, next_retry_at, id)
);

INSERT INTO product_sales_daily
    (product_id, metric_date, sales_quantity, source, source_reference, created_at, updated_at)
SELECT id,
       CURRENT_DATE - INTERVAL MOD(id, 20) DAY,
       8 + MOD(id * 17, 120),
       'SEED',
       'supermarket-v2-demo',
       CURRENT_TIMESTAMP(3),
       CURRENT_TIMESTAMP(3)
FROM products
WHERE deleted = FALSE;

INSERT INTO product_search_outbox
    (product_id, event_type, product_version, status, retry_count, created_at)
SELECT id, 'UPSERT', version, 'NEW', 0, CURRENT_TIMESTAMP(3)
FROM products;
