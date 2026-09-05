package com.leisure.festival.domain;

public enum FestivalCategory {

    FESTIVAL("EV01"),

    PERFORMANCE("EV02"),

    EVENT("EV03");

    private final String code;

    FestivalCategory(String code) {
            this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static FestivalCategory fromCode(String code) {

        for (FestivalCategory category : values()) {

            if (category.code.equals(code)) {
                return category;
            }
        }
        return null;
    }
}
