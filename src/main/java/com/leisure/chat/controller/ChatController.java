package com.leisure.chat.controller;

import com.leisure.chat.dto.ChatMessageResponse;
import com.leisure.chat.dto.ChatRequest;
import com.leisure.chat.dto.ChatRoomResponse;
import com.leisure.chat.service.ChatService;
import com.leisure.global.auth.CurrentMember;
import com.leisure.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

@Tag(name = "채팅(Chat)", description = "AI 챗봇 대화")
@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(
            summary = "AI 챗봇에 질문 (SSE 스트리밍)",
            description = """
                    질문을 보내면 답변을 토큰 단위로 SSE(`text/event-stream`)로 흘려보낸다.
                    - `roomId`가 null이면 새 대화방을 생성하고, 값이 있으면 해당 방에 이어서 묻는다(본인 방만).
                    - 새로 생성된 방의 id는 응답 헤더 `X-Chat-Room-Id`로 내려준다.
                    - 직전 대화 최대 6개(3턴)를 컨텍스트로 함께 전달한다.
                    - 정상 완료된 답변만 저장되며, 중간 에러·연결 끊김 시 답변은 저장하지 않는다.
                    """
    )
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping(value = "/chats", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(
            @CurrentMember String publicId,
            @Valid @RequestBody ChatRequest request,
            HttpServletResponse response) {

        ChatService.Prepared prepared = chatService.prepare(publicId, request);
        response.setHeader("X-Chat-Room-Id", String.valueOf(prepared.roomId()));

        return chatService.stream(prepared);
    }

    @Operation(summary = "내 대화방 목록", description = "최근 대화 순으로 내 대화방을 조회한다.")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/chats")
    public ResponseEntity<ApiResponse<List<ChatRoomResponse>>> myRooms(@CurrentMember String publicId) {

        List<ChatRoomResponse> response = chatService.getMyRooms(publicId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("대화방 목록 조회에 성공했습니다.", response));
    }

    @Operation(summary = "대화방 메시지 조회", description = "본인 대화방의 메시지를 시간순으로 조회한다. 남의 방은 404.")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/chats/{roomId}")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> messages(@CurrentMember String publicId, @PathVariable Long roomId) {

        List<ChatMessageResponse> response = chatService.getMessages(publicId, roomId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("대화 메시지 조회에 성공했습니다.", response));
    }

    @Operation(summary = "대화방 삭제", description = "본인 대화방과 메시지를 함께 삭제한다. 남의 방은 404.")
    @SecurityRequirement(name = "BearerAuth")
    @DeleteMapping("/chats/{roomId}")
    public ResponseEntity<Void> deleteRoom(@CurrentMember String publicId, @PathVariable Long roomId) {

        chatService.deleteRoom(publicId, roomId);

        return ResponseEntity.noContent().build();
    }
}
