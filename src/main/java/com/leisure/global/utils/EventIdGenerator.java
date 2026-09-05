package com.leisure.global.utils;

import java.util.UUID;

public final class EventIdGenerator {

    private EventIdGenerator() {}

    public static String generate() {
        return UUID.randomUUID().toString();
    }
}
