package com.sahastra.backend.domain.enums;

/**
 * User role enumeration for Role-Based Access Control (RBAC).
 */
public enum UserRole {
    ROLE_CUSTOMER("Customer"),
    ROLE_ADMIN("Administrator");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
