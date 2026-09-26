package com.sahastra.backend.domain.enums;

/**
 * Token type enumeration for different token purposes.
 */
public enum TokenType {
    ACCESS("Access token", 15), // 15 minutes default
    REFRESH("Refresh token", 7 * 24 * 60), // 7 days default
    REMEMBER_ME("Remember-me token", 30 * 24 * 60); // 30 days default

    private final String description;
    private final int defaultExpiryMinutes;

    TokenType(String description, int defaultExpiryMinutes) {
        this.description = description;
        this.defaultExpiryMinutes = defaultExpiryMinutes;
    }

    public String getDescription() {
        return description;
    }

    public int getDefaultExpiryMinutes() {
        return defaultExpiryMinutes;
    }
}
