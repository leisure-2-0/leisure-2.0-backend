package com.leisure.chat.repository;

import com.leisure.chat.domain.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByChatRoomIdAndMemberId(Long chatRoomId, Long memberId);

    List<ChatRoom> findByMemberIdOrderByUpdatedAtDesc(Long memberId);

    void deleteByChatRoomId(Long chatRoomId);
}
