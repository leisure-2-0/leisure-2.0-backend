package com.leisure.chat.repository;

import com.leisure.chat.domain.ChatMessage;
import com.leisure.chat.domain.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findTop6ByChatRoomIdAndMessageStatusOrderByCreatedAtDesc(Long chatRoomId, MessageStatus messageStatus);

    List<ChatMessage> findByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);

    @Modifying
    @Query("delete from ChatMessage m where m.chatRoomId = :chatRoomId")
    void deleteByChatRoomId(Long chatRoomId);

    // 탈퇴 회원 퍼지: 여러 대화방의 메시지 벌크 삭제
    @Modifying
    @Query("delete from ChatMessage m where m.chatRoomId in :chatRoomIds")
    void deleteByChatRoomIdIn(java.util.Collection<Long> chatRoomIds);
}
