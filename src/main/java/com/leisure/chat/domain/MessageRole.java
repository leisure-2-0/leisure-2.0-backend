package com.leisure.chat.domain;

public enum MessageRole {

    USER("user"),

    ASSISTANT("assistant");

    private final String apiRole;

    MessageRole(String apiRole) {
        this.apiRole = apiRole;
    }

    public String getApiRole() {
        return apiRole;
    }
}
