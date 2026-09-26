package com.sahastra.backend.domain.enums;

/**
 * User status enumeration.
 * Controls account activation and suspension.
 */
public enum UserStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive - Email not verified"),
    SUSPENDED("Account suspended");

    private final String description;

    UserStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
