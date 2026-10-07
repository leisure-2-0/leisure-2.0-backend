package com.leisure.chat.dto;

import com.leisure.chat.domain.ChatRoom;

import java.time.LocalDateTime;

public record ChatRoomResponse(
        Long roomId,
        String title,
        LocalDateTime updatedAt
) {
    public static ChatRoomResponse from(ChatRoom room) {
        return new ChatRoomResponse(room.getChatRoomId(), room.getTitle(), room.getUpdatedAt());
    }
}
