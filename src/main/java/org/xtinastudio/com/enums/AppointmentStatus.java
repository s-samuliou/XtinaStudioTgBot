package org.xtinastudio.com.enums;

public enum AppointmentStatus {
    BANNED("Banned"),
    COMPLETED("Completed"),
    CANCELED("Canceled");

    private final String description;

    AppointmentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
