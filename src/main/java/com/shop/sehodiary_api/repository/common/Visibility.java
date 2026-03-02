package com.shop.sehodiary_api.repository.common;

public enum Visibility {
    PUBLIC,
    PRIVATE,
    FRIENDS;

    public static Visibility from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("visibility is required");
        }
        try {
            return Visibility.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid visibility: " + value);
        }
    }
}
