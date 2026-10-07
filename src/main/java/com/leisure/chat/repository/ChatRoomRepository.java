package com.leisure.chat.repository;

import com.leisure.chat.domain.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByChatRoomIdAndMemberId(Long chatRoomId, Long memberId);

    List<ChatRoom> findByMemberIdOrderByUpdatedAtDesc(Long memberId);

    void deleteByChatRoomId(Long chatRoomId);

    // 탈퇴 회원 퍼지: 회원들의 대화방 id 조회(메시지 삭제용) + 대화방 벌크 삭제
    @Query("select r.chatRoomId from ChatRoom r where r.memberId in :memberIds")
    List<Long> findChatRoomIdsByMemberIdIn(Collection<Long> memberIds);

    @Modifying
    @Query("delete from ChatRoom r where r.memberId in :memberIds")
    void deleteByMemberIdIn(Collection<Long> memberIds);
}
