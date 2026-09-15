package com.leisure.chat.domain;

import com.leisure.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_rooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ChatRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_id")
    private Long chatRoomId;

    @Column(name = "member_id", nullable = false, updatable = false, comment = "방 소유자 회원 ID (id-only 참조, FK 없음)")
    private Long memberId;

    @Column(name = "title", nullable = false, comment = "대화 제목 (첫 질문 요약, 목록 표시용)")
    private String title;

    private ChatRoom(Long memberId, String title) {
        this.memberId = memberId;
        this.title = title;
    }

    public static ChatRoom create(Long memberId, String title) {
        return new ChatRoom(memberId, title);
    }
}
