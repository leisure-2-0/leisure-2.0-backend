package com.leisure.chat.repository;

import com.leisure.chat.domain.ChatMessage;
import com.leisure.chat.domain.MessageStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findTop6ByChatRoomIdAndMessageStatusOrderByCreatedAtDesc(Long chatRoomId, MessageStatus messageStatus);
}
