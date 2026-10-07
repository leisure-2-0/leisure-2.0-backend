package com.leisure.chat.dto;

import com.leisure.chat.domain.ChatMessage;
import com.leisure.chat.domain.MessageRole;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        MessageRole role,
        String content,
        LocalDateTime createdAt
) {
    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(message.getMessageRole(), message.getContent(), message.getCreatedAt());
    }
}
