package org.xtinastudio.com.enums;

public enum WorkTime {

    EIGHT("8:00"),
    EIGHT_FIFTEEN("8:15"),
    EIGHT_THIRTY("8:30"),
    EIGHT_FORTY_FIVE("8:45"),

    NINE("9:00"),
    NINE_FIFTEEN("9:15"),
    NINE_THIRTY("9:30"),
    NINE_FORTY_FIVE("9:45"),

    TEN("10:00"),
    TEN_FIFTEEN("10:15"),
    TEN_THIRTY("10:30"),
    TEN_FORTY_FIVE("10:45"),

    ELEVEN("11:00"),
    ELEVEN_FIFTEEN("11:15"),
    ELEVEN_THIRTY("11:30"),
    ELEVEN_FORTY_FIVE("11:45"),

    TWELVE("12:00"),
    TWELVE_FIFTEEN("12:15"),
    TWELVE_THIRTY("12:30"),
    TWELVE_FORTY_FIVE("12:45"),

    THIRTEEN("13:00"),
    THIRTEEN_FIFTEEN("13:15"),
    THIRTEEN_THIRTY("13:30"),
    THIRTEEN_FORTY_FIVE("13:45"),

    FOURTEEN("14:00"),
    FOURTEEN_FIFTEEN("14:15"),
    FOURTEEN_THIRTY("14:30"),
    FOURTEEN_FORTY_FIVE("14:45"),

    FIFTEEN("15:00"),
    FIFTEEN_FIFTEEN("15:15"),
    FIFTEEN_THIRTY("15:30"),
    FIFTEEN_FORTY_FIVE("15:45"),

    SIXTEEN("16:00"),
    SIXTEEN_FIFTEEN("16:15"),
    SIXTEEN_THIRTY("16:30"),
    SIXTEEN_FORTY_FIVE("16:45"),

    SEVENTEEN("17:00"),
    SEVENTEEN_FIFTEEN("17:15"),
    SEVENTEEN_THIRTY("17:30"),
    SEVENTEEN_FORTY_FIVE("17:45"),

    EIGHTEEN("18:00"),
    EIGHTEEN_FIFTEEN("18:15"),
    EIGHTEEN_THIRTY("18:30"),
    EIGHTEEN_FORTY_FIVE("18:45"),

    NINETEEN("19:00");

    private final String description;

    WorkTime(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
