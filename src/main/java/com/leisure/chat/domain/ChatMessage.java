package com.leisure.chat.domain;

import com.leisure.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ChatMessage extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_message_id")
    private Long chatMessageId;

    @Column(name = "chat_room_id", nullable = false, updatable = false, comment = "소속 대화방 ID (id-only 참조, FK 없음)")
    private Long chatRoomId;

    @Column(name = "content", nullable = false, updatable = false, columnDefinition = "TEXT", comment = "메시지 내용")
    private String content;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "message_role", nullable = false, updatable = false, comment = "화자 (USER/ASSISTANT)")
    private MessageRole messageRole;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "message_status", nullable = false, updatable = false, comment = "스트림 상태 (완료/실패/중단)")
    private MessageStatus messageStatus;


    private ChatMessage(Long chatRoomId, String content, MessageRole messageRole, MessageStatus messageStatus) {
        this.chatRoomId = chatRoomId;
        this.content = content;
        this.messageRole = messageRole;
        this.messageStatus = messageStatus;
    }

    public static ChatMessage create(Long chatRoomId, String content, MessageRole messageRole, MessageStatus messageStatus) {
        return new ChatMessage(chatRoomId, content, messageRole, messageStatus);
    }
}
