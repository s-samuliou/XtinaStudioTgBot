package org.xtinastudio.com.enums;

public enum ReportTimePeriod {

    MONTH("Месяц"),
    YEAR("Год"),
    AllTIME("Всё время");

    private final String description;

    ReportTimePeriod(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
