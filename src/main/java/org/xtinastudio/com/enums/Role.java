package org.xtinastudio.com.enums;

public enum Role {
    MASTER("ROLE_MASTER"),
    ADMIN("ROLE_ADMIN"),
    MASTER_ADMIN("ROLE_MASTER_ADMIN");

    private final String description;

    Role(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
