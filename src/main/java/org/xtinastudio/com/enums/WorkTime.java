package org.xtinastudio.com.enums;

public enum WorkTime {

   /* EIGHT("8:00"),*/
    NINE("9:00"),
    TEN("10:00"),
    ELEVEN("11:00"),
    TWELVE("12:00");

    private final String description;

    WorkTime(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
