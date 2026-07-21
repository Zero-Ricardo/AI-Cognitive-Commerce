package com.aishop.commerce.domain;

public final class Enums {
    private Enums() {}

    public enum UserStatus { ACTIVE, DISABLED }
    public enum Role { USER, ADMIN }
    public enum CommonStatus { ENABLED, DISABLED }
    public enum ProductStatus { DRAFT, ON_SALE, OFF_SALE }
    public enum ProductEventType { VIEW, FAVORITE, ADD_CART }
}
