package org.xtinastudio.com.enums;

public enum WorkStatus {
    SICK("Больничный"),
    VACATION("Отпуск"),
    DAYOFF("Выходной"),
    WORKING("Работает");

    private final String description;

    WorkStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
