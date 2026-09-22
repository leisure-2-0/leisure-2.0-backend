package com.leisure.image.domain;

public enum ImagePurpose {

    PROFILE("profiles"),

    THUMBNAIL("thumbnails"),

    CONTENT("contents");

    private final String prefix;

    ImagePurpose(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}
