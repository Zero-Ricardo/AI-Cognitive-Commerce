CREATE TABLE user_addresses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    recipient_name VARCHAR(64) NOT NULL,
    recipient_phone VARCHAR(32) NOT NULL,
    province VARCHAR(64) NOT NULL,
    city VARCHAR(64) NOT NULL,
    district VARCHAR(64) NOT NULL,
    detail_address VARCHAR(255) NOT NULL,
    postal_code VARCHAR(16) NULL,
    default_address BOOLEAN NOT NULL DEFAULT FALSE,
    label VARCHAR(32) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    CONSTRAINT fk_user_address_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_user_address_user (user_id, default_address)
);

CREATE TABLE seed_datasets (
    dataset_id VARCHAR(64) PRIMARY KEY,
    version VARCHAR(32) NOT NULL,
    description VARCHAR(500) NULL,
    imported_at DATETIME(3) NOT NULL
);

