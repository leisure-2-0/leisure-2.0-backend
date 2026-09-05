package com.leisure.global.event.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public record EventEnvelope<T>(

        @JsonProperty("event_id")
        String eventId,

        @JsonProperty("event_type")
        EventType eventType,

        @JsonProperty("correlation_id")
        String correlationId,

        @JsonProperty("occurred_at")
        OffsetDateTime occurredAt,

        String version,

        T data
) {
    public static <T> EventEnvelope<T> of(String eventId, EventType eventType, String correlationId, T data) {
        return new EventEnvelope<>(eventId, eventType, correlationId, OffsetDateTime.now(), "1.0", data);
    }
}
