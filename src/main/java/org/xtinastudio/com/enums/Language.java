package org.xtinastudio.com.enums;

public enum Language {

    ENGLISH("English"),
    RUSSIAN("Russian");

    private final String description;

    Language(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
