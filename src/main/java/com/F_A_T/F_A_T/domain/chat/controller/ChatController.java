package com.F_A_T.F_A_T.domain.chat.controller;

import com.F_A_T.F_A_T.domain.chat.dto.request.ChatMessageCreateRequest;
import com.F_A_T.F_A_T.domain.chat.dto.response.ChatMessageResponse;
import com.F_A_T.F_A_T.domain.chat.dto.response.ChatRoomResponse;
import com.F_A_T.F_A_T.domain.chat.service.ChatService;
import com.F_A_T.F_A_T.domain.user.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat-rooms")
public class ChatController {

    private final ChatService chatService;

    @GetMapping
    public ResponseEntity<List<ChatRoomResponse>> getMyChatRooms() {
        return ResponseEntity.ok(chatService.getMyChatRooms());
    }

    @GetMapping("/{roomId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getMessages(@PathVariable Long roomId) {
        return ResponseEntity.ok(chatService.getMessages(roomId));
    }

    @PostMapping("/{roomId}/messages")
    public ResponseEntity<Long> sendMessage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId,
            @RequestBody @Valid ChatMessageCreateRequest request) {

        Long messageId = chatService.sendMessage(userDetails.getUser(), roomId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(messageId);
    }
}