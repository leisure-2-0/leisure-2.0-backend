package com.leisure.global.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("search")
public record SearchProperties(

        boolean enabled,

        Sync sync,

        InitialIndex initialIndex,

        Boost boost,

        String fuzziness,

        Fuzz fuzz) {

    public record Sync(

            long fixedDelayMs,

            int batchSize) {
    }

    public record InitialIndex(

            boolean enabled
    ) {
    }

    public record Boost(

            Float title,

            Float tags,

            Float category,

            Float region
    ) {
    }

    public record Fuzz(

            int maxExpansions,

            int prefixLength
    ) {
    }
}
