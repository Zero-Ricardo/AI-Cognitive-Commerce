CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    phone VARCHAR(32) NULL,
    email VARCHAR(128) NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_phone UNIQUE (phone),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE session_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    access_token_hash CHAR(64) NOT NULL,
    refresh_token_hash CHAR(64) NOT NULL,
    access_expires_at DATETIME(3) NOT NULL,
    refresh_expires_at DATETIME(3) NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(3) NOT NULL,
    CONSTRAINT uk_session_access UNIQUE (access_token_hash),
    CONSTRAINT uk_session_refresh UNIQUE (refresh_token_hash),
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id BIGINT NULL,
    name VARCHAR(80) NOT NULL,
    level INT NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES categories(id)
);

CREATE TABLE brands (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(80) NOT NULL,
    logo_url VARCHAR(512) NULL,
    description VARCHAR(500) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    CONSTRAINT uk_brand_name UNIQUE (name)
);

CREATE TABLE products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_no VARCHAR(64) NOT NULL,
    name VARCHAR(200) NOT NULL,
    subtitle VARCHAR(300) NULL,
    category_id BIGINT NOT NULL,
    brand_id BIGINT NULL,
    sale_price DECIMAL(12,2) NOT NULL,
    original_price DECIMAL(12,2) NULL,
    stock INT NOT NULL DEFAULT 0,
    main_image_url VARCHAR(512) NOT NULL,
    image_urls_json JSON NULL,
    description TEXT NOT NULL,
    keywords VARCHAR(500) NULL,
    scenarios VARCHAR(500) NULL,
    audiences VARCHAR(500) NULL,
    specification_json JSON NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_at DATETIME(3) NULL,
    version INT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    CONSTRAINT uk_product_no UNIQUE (product_no),
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT fk_product_brand FOREIGN KEY (brand_id) REFERENCES brands(id),
    INDEX idx_product_filter (status, deleted, category_id, brand_id),
    INDEX idx_product_price (sale_price),
    INDEX idx_product_published (published_at)
);

CREATE TABLE favorites (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL,
    CONSTRAINT uk_favorite_user_product UNIQUE (user_id, product_id),
    CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_favorite_product FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_favorite_user_time (user_id, created_at)
);

CREATE TABLE cart_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    selected BOOLEAN NOT NULL DEFAULT TRUE,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    CONSTRAINT uk_cart_user_product UNIQUE (user_id, product_id),
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_cart_user_time (user_id, updated_at)
);

CREATE TABLE product_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NULL,
    anonymous_id VARCHAR(64) NULL,
    product_id BIGINT NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    score INT NOT NULL,
    occurred_at DATETIME(3) NOT NULL,
    CONSTRAINT fk_event_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_event_product FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_event_product_type_time (product_id, event_type, occurred_at)
);

CREATE TABLE admin_audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    admin_user_id BIGINT NOT NULL,
    action VARCHAR(64) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id VARCHAR(64) NOT NULL,
    change_summary VARCHAR(1000) NULL,
    request_id VARCHAR(64) NULL,
    created_at DATETIME(3) NOT NULL,
    CONSTRAINT fk_audit_admin FOREIGN KEY (admin_user_id) REFERENCES users(id)
);
