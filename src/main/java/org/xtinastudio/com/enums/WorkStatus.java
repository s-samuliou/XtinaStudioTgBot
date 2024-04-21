package org.xtinastudio.com.enums;

public enum WorkStatus {
    SICK("SICK"),
    VACATION("VACATION"),
    DAY_OFF("DAY_OFF"),
    WORKING("WORKING");

    private final String description;

    WorkStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
