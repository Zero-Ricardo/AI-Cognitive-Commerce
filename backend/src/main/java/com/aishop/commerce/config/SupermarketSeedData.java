package com.aishop.commerce.config;

import java.util.List;
import java.util.Map;

record SupermarketSeedData(
        Manifest manifest,
        List<CategorySeed> categories,
        List<BrandSeed> brands,
        List<UserSeed> users,
        List<AddressSeed> addresses,
        List<ProductSeed> products
) {
    record Manifest(String id, String version, String locale, String currency, String description) {}
    record CategorySeed(String code, String parentCode, String name, int level, int sortOrder, String status) {}
    record BrandSeed(String code, String name, String description, int sortOrder, String status) {}
    record UserSeed(String username, String nickname, String phone, String email, List<String> roles) {}
    record AddressSeed(String username, String recipientName, String recipientPhone, String province, String city,
                       String district, String detailAddress, String postalCode, boolean defaultAddress, String label) {}
    record ProductSeed(String productNo, String name, String subtitle, String categoryCode, String brandCode,
                       String salePrice, String originalPrice, int stock, String mainImageUrl, String localImage,
                       List<String> imageUrls, String description, String keywords, String scenarios, String audiences,
                       Map<String, String> specifications, String status, int publishedOffsetDays) {}
}

