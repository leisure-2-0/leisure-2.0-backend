package com.leisure.chat.service;

import com.leisure.chat.domain.ChatMessage;
import com.leisure.chat.domain.ChatRoom;
import com.leisure.chat.domain.MessageRole;
import com.leisure.chat.dto.ChatRequest;
import com.leisure.chat.repository.ChatMessageRepository;
import com.leisure.chat.repository.ChatRoomRepository;
import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.member.domain.Member;
import com.leisure.member.service.MemberReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    private static final String PUBLIC_ID = "pub-123";
    private static final Long MEMBER_ID = 1L;
    private static final Long ROOM_ID = 100L;

    @Mock
    private MemberReader memberReader;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private WebClient aiWebClient;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(memberReader, chatRoomRepository, chatMessageRepository, aiWebClient);

        Member member = mock(Member.class);
        given(member.getMemberId()).willReturn(MEMBER_ID);
        given(memberReader.getMemberByPublicId(PUBLIC_ID)).willReturn(member);
    }

    @Test
    @DisplayName("roomId가 null이면 새 방을 만들고 user 메시지를 저장한다")
    void prepare_newRoom() {
        ChatRoom saved = mock(ChatRoom.class);
        given(saved.getChatRoomId()).willReturn(ROOM_ID);
        given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(saved);

        ChatService.Prepared prepared = chatService.prepare(PUBLIC_ID, new ChatRequest("제주 추천해줘", null));

        assertThat(prepared.roomId()).isEqualTo(ROOM_ID);
        assertThat(prepared.aiRequest().question()).isEqualTo("제주 추천해줘");
        assertThat(prepared.aiRequest().history()).isEmpty();   // 새 방이라 이력 없음

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(captor.capture());
        assertThat(captor.getValue().getMessageRole()).isEqualTo(MessageRole.USER);
    }

    @Test
    @DisplayName("기존 방이면 소유 검증 후 그 방에 이어간다")
    void prepare_existingRoom_owned() {
        ChatRoom room = mock(ChatRoom.class);
        given(room.getChatRoomId()).willReturn(ROOM_ID);
        given(chatRoomRepository.findByChatRoomIdAndMemberId(ROOM_ID, MEMBER_ID)).willReturn(Optional.of(room));

        ChatService.Prepared prepared = chatService.prepare(PUBLIC_ID, new ChatRequest("더 알려줘", ROOM_ID));

        assertThat(prepared.roomId()).isEqualTo(ROOM_ID);
        verify(chatRoomRepository, never()).save(any());   // 새 방 안 만듦
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("남의 방에 이어가려 하면 CHAT_ROOM_NOT_FOUND")
    void prepare_existingRoom_notOwned() {
        given(chatRoomRepository.findByChatRoomIdAndMemberId(ROOM_ID, MEMBER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.prepare(PUBLIC_ID, new ChatRequest("q", ROOM_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("남의 방 메시지 조회 시 CHAT_ROOM_NOT_FOUND")
    void getMessages_notOwned() {
        given(chatRoomRepository.findByChatRoomIdAndMemberId(ROOM_ID, MEMBER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.getMessages(PUBLIC_ID, ROOM_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("본인 방 삭제 시 메시지 → 방 순으로 삭제한다")
    void deleteRoom_owned() {
        given(chatRoomRepository.findByChatRoomIdAndMemberId(ROOM_ID, MEMBER_ID))
                .willReturn(Optional.of(mock(ChatRoom.class)));

        chatService.deleteRoom(PUBLIC_ID, ROOM_ID);

        verify(chatMessageRepository).deleteByChatRoomId(ROOM_ID);
        verify(chatRoomRepository).deleteByChatRoomId(ROOM_ID);
    }

    @Test
    @DisplayName("남의 방 삭제 시 CHAT_ROOM_NOT_FOUND")
    void deleteRoom_notOwned() {
        given(chatRoomRepository.findByChatRoomIdAndMemberId(ROOM_ID, MEMBER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.deleteRoom(PUBLIC_ID, ROOM_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND);
    }
}
