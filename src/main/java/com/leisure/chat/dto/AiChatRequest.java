package com.leisure.chat.dto;

import java.util.List;

public record AiChatRequest(
        String question,
        List<Turn> history
) {
    public record Turn(String role, String content) {}
}
