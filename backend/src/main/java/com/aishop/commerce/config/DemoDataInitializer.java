package com.aishop.commerce.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DemoDataInitializer implements ApplicationRunner {
    private static final String DATASET_RESOURCE = "seed/supermarket-v1/dataset.json";
    private static final List<String> LEGACY_PRODUCTS = List.of("CAM-001", "CAM-002", "CAM-003", "AUD-001", "AUD-002", "PHO-001");

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String adminPassword;
    private final Path storagePath;

    public DemoDataInitializer(JdbcTemplate jdbc, ObjectMapper objectMapper, PasswordEncoder passwordEncoder,
                               @Value("${app.demo-data.enabled:true}") boolean enabled,
                               @Value("${APP_ADMIN_PASSWORD:Admin@123456}") String adminPassword,
                               @Value("${app.storage.path}") String storagePath) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.adminPassword = adminPassword;
        this.storagePath = Path.of(storagePath).toAbsolutePath().normalize();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (!enabled) return;
        SupermarketSeedData data = readDataset();
        // The database and upload directory have independent Docker volumes. Always restore
        // bundled seed images so an existing database can recover from a new or cleared volume.
        copyProductImages(data.products());
        Integer imported = jdbc.queryForObject("SELECT COUNT(*) FROM seed_datasets WHERE dataset_id = ?", Integer.class, data.manifest().id());
        if (imported != null && imported > 0) return;

        removeLegacyCatalogWhenSafe();
        Map<String, Long> userIds = importUsers(data.users());
        Map<String, Long> categoryIds = importCategories(data.categories());
        Map<String, Long> brandIds = importBrands(data.brands());
        importAddresses(data.addresses(), userIds);
        importProducts(data.products(), categoryIds, brandIds);
        jdbc.update("INSERT INTO seed_datasets(dataset_id, version, description, imported_at) VALUES (?, ?, ?, ?)",
                data.manifest().id(), data.manifest().version(), data.manifest().description(), Timestamp.from(Instant.now()));
    }

    private SupermarketSeedData readDataset() throws IOException {
        try (var input = new ClassPathResource(DATASET_RESOURCE).getInputStream()) {
            return objectMapper.readValue(input, SupermarketSeedData.class);
        }
    }

    private void removeLegacyCatalogWhenSafe() {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM products", Integer.class);
        if (total == null || total == 0) return;
        String placeholders = String.join(",", java.util.Collections.nCopies(LEGACY_PRODUCTS.size(), "?"));
        Integer legacy = jdbc.queryForObject("SELECT COUNT(*) FROM products WHERE product_no IN (" + placeholders + ")",
                Integer.class, LEGACY_PRODUCTS.toArray());
        if (!total.equals(legacy)) return;
        jdbc.update("DELETE FROM product_events");
        jdbc.update("DELETE FROM favorites");
        jdbc.update("DELETE FROM cart_items");
        jdbc.update("DELETE FROM products");
        jdbc.update("DELETE FROM brands");
        jdbc.update("DELETE FROM categories WHERE parent_id IS NOT NULL");
        jdbc.update("DELETE FROM categories WHERE parent_id IS NULL");
    }

    private Map<String, Long> importUsers(List<SupermarketSeedData.UserSeed> seeds) {
        Map<String, Long> result = new HashMap<>();
        Instant now = Instant.now();
        for (var seed : seeds) {
            List<Long> existing = jdbc.query("SELECT id FROM users WHERE LOWER(username) = LOWER(?)",
                    (rs, row) -> rs.getLong(1), seed.username());
            long id;
            if (existing.isEmpty()) {
                String password = seed.username().equals("admin") ? adminPassword : "User@123456";
                jdbc.update("INSERT INTO users(username, phone, email, password_hash, nickname, status, version, created_at, updated_at) VALUES (?, ?, ?, ?, ?, 'ACTIVE', 0, ?, ?)",
                        seed.username(), seed.phone(), seed.email(), passwordEncoder.encode(password), seed.nickname(),
                        Timestamp.from(now), Timestamp.from(now));
                id = jdbc.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, seed.username());
            } else {
                id = existing.getFirst();
                jdbc.update("UPDATE users SET phone = COALESCE(phone, ?), email = COALESCE(email, ?), nickname = ?, updated_at = ? WHERE id = ?",
                        seed.phone(), seed.email(), seed.nickname(), Timestamp.from(now), id);
            }
            for (String role : seed.roles()) {
                jdbc.update("INSERT IGNORE INTO user_roles(user_id, role) VALUES (?, ?)", id, role);
            }
            result.put(seed.username(), id);
        }
        return result;
    }

    private Map<String, Long> importCategories(List<SupermarketSeedData.CategorySeed> seeds) {
        Map<String, Long> ids = new HashMap<>();
        Instant now = Instant.now();
        seeds.stream().sorted(java.util.Comparator.comparingInt(SupermarketSeedData.CategorySeed::level)).forEach(seed -> {
            Long parentId = seed.parentCode() == null ? null : ids.get(seed.parentCode());
            jdbc.update("INSERT INTO categories(parent_id, name, level, sort_order, status, deleted, created_at, updated_at) VALUES (?, ?, ?, ?, ?, false, ?, ?)",
                    parentId, seed.name(), seed.level(), seed.sortOrder(), seed.status(), Timestamp.from(now), Timestamp.from(now));
            Long id = jdbc.queryForObject("SELECT id FROM categories WHERE name = ? AND deleted = false ORDER BY id DESC LIMIT 1", Long.class, seed.name());
            ids.put(seed.code(), id);
        });
        return ids;
    }

    private Map<String, Long> importBrands(List<SupermarketSeedData.BrandSeed> seeds) {
        Map<String, Long> ids = new HashMap<>();
        Instant now = Instant.now();
        for (var seed : seeds) {
            jdbc.update("INSERT INTO brands(name, description, sort_order, status, deleted, created_at, updated_at) VALUES (?, ?, ?, ?, false, ?, ?)",
                    seed.name(), seed.description(), seed.sortOrder(), seed.status(), Timestamp.from(now), Timestamp.from(now));
            Long id = jdbc.queryForObject("SELECT id FROM brands WHERE name = ?", Long.class, seed.name());
            ids.put(seed.code(), id);
        }
        return ids;
    }

    private void importAddresses(List<SupermarketSeedData.AddressSeed> seeds, Map<String, Long> users) {
        Instant now = Instant.now();
        for (var seed : seeds) {
            jdbc.update("INSERT INTO user_addresses(user_id, recipient_name, recipient_phone, province, city, district, detail_address, postal_code, default_address, label, version, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?)",
                    users.get(seed.username()), seed.recipientName(), seed.recipientPhone(), seed.province(), seed.city(),
                    seed.district(), seed.detailAddress(), seed.postalCode(), seed.defaultAddress(), seed.label(),
                    Timestamp.from(now), Timestamp.from(now));
        }
    }

    private void importProducts(List<SupermarketSeedData.ProductSeed> seeds, Map<String, Long> categories,
                                Map<String, Long> brands) throws IOException {
        Instant now = Instant.now();
        for (var seed : seeds) {
            jdbc.update("INSERT INTO products(product_no, name, subtitle, category_id, brand_id, sale_price, original_price, stock, main_image_url, image_urls_json, description, keywords, scenarios, audiences, specification_json, status, published_at, version, deleted, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, false, ?, ?)",
                    seed.productNo(), seed.name(), seed.subtitle(), categories.get(seed.categoryCode()), brands.get(seed.brandCode()),
                    seed.salePrice(), seed.originalPrice(), seed.stock(), seed.mainImageUrl(), objectMapper.writeValueAsString(seed.imageUrls()),
                    seed.description(), seed.keywords(), seed.scenarios(), seed.audiences(), objectMapper.writeValueAsString(seed.specifications()),
                    seed.status(), Timestamp.from(now.minus(seed.publishedOffsetDays(), ChronoUnit.HOURS)), Timestamp.from(now), Timestamp.from(now));
        }
    }

    private void copyProductImages(List<SupermarketSeedData.ProductSeed> products) throws IOException {
        Path targetDirectory = storagePath.resolve("seed/supermarket-v1");
        Files.createDirectories(targetDirectory);
        for (var product : products) {
            String filename = Path.of(product.localImage()).getFileName().toString();
            Path target = targetDirectory.resolve(filename);
            try (var input = new ClassPathResource("seed/supermarket-v1/" + product.localImage()).getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
