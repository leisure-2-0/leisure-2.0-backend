package com.leisure.chat.service;

import com.leisure.chat.domain.ChatMessage;
import com.leisure.chat.domain.ChatRoom;
import com.leisure.chat.domain.MessageRole;
import com.leisure.chat.domain.MessageStatus;
import com.leisure.chat.dto.AiChatRequest;
import com.leisure.chat.dto.AiChatResponse;
import com.leisure.chat.dto.ChatMessageResponse;
import com.leisure.chat.dto.ChatRequest;
import com.leisure.chat.dto.ChatRoomResponse;
import com.leisure.chat.repository.ChatMessageRepository;
import com.leisure.chat.repository.ChatRoomRepository;
import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.member.service.MemberReader;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final MemberReader memberReader;

    private final ChatRoomRepository chatRoomRepository;

    private final ChatMessageRepository chatMessageRepository;

    private final WebClient aiWebClient;

    public record Prepared(Long roomId, AiChatRequest aiRequest) {}

    @Transactional
    public Prepared prepare(String publicId, ChatRequest request) {
        Long memberId = memberReader.getMemberByPublicId(publicId).getMemberId();

        Long roomId;
        if (request.roomId() == null) {
            roomId = chatRoomRepository.save(ChatRoom.create(memberId, toTitle(request.question()))).getChatRoomId();
        } else {
            roomId = chatRoomRepository.findByChatRoomIdAndMemberId(request.roomId(), memberId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND))
                    .getChatRoomId();
        }

        List<AiChatRequest.Turn> history = buildHistory(roomId);

        chatMessageRepository.save(
                ChatMessage.create(roomId, request.question(), MessageRole.USER, MessageStatus.COMPLETED));

        return new Prepared(roomId, new AiChatRequest(request.question(), history));
    }

    public Flux<ServerSentEvent<String>> stream(Prepared prepared) {
        StringBuilder answer = new StringBuilder();

        return aiWebClient.post()
                .uri("/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(prepared.aiRequest())
                .retrieve()
                .bodyToFlux(AiChatResponse.class)
                .filter(event -> event instanceof AiChatResponse.Token)
                .map(event -> ((AiChatResponse.Token) event).content())
                .doOnNext(answer::append)
                .map(content -> ServerSentEvent.builder(content).build())
                .doOnComplete(() -> saveAssistantAsync(prepared.roomId(), answer.toString()))
                .doOnError(e -> log.error("[chat] AI 스트림 실패 roomId={}", prepared.roomId(), e));
    }

    private List<AiChatRequest.Turn> buildHistory(Long roomId) {
        List<ChatMessage> recent = chatMessageRepository
                .findTop6ByChatRoomIdAndMessageStatusOrderByCreatedAtDesc(roomId, MessageStatus.COMPLETED);
        List<ChatMessage> chronological = new ArrayList<>(recent);
        Collections.reverse(chronological);
        return chronological.stream()
                .map(m -> new AiChatRequest.Turn(m.getMessageRole().getApiRole(), m.getContent()))
                .toList();
    }

    private void saveAssistantAsync(Long roomId, String content) {
        if (content.isBlank()) {
            return;
        }
        Schedulers.boundedElastic().schedule(() ->
                chatMessageRepository.save(
                        ChatMessage.create(roomId, content, MessageRole.ASSISTANT, MessageStatus.COMPLETED)));
    }

    private String toTitle(String question) {
        String trimmed = question.strip();
        return trimmed.length() <= 30 ? trimmed : trimmed.substring(0, 30);
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getMyRooms(String publicId) {
        Long memberId = memberReader.getMemberByPublicId(publicId).getMemberId();
        return chatRoomRepository.findByMemberIdOrderByUpdatedAtDesc(memberId).stream()
                .map(ChatRoomResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(String publicId, Long roomId) {
        Long memberId = memberReader.getMemberByPublicId(publicId).getMemberId();
        chatRoomRepository.findByChatRoomIdAndMemberId(roomId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        return chatMessageRepository.findByChatRoomIdOrderByCreatedAtAsc(roomId).stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    @Transactional
    public void deleteRoom(String publicId, Long roomId) {
        Long memberId = memberReader.getMemberByPublicId(publicId).getMemberId();
        chatRoomRepository.findByChatRoomIdAndMemberId(roomId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        chatMessageRepository.deleteByChatRoomId(roomId);
        chatRoomRepository.deleteByChatRoomId(roomId);
    }
}
