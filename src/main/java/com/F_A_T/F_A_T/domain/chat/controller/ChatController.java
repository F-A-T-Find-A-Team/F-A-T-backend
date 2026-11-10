package com.F_A_T.F_A_T.domain.chat.controller;

import com.F_A_T.F_A_T.domain.chat.dto.request.*;
import com.F_A_T.F_A_T.domain.chat.dto.response.ChatApiResponse;
import com.F_A_T.F_A_T.domain.chat.service.ChatService;
import com.F_A_T.F_A_T.global.dto.ApiResponse;
import com.F_A_T.F_A_T.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat-rooms")
public class ChatController {
    private final ChatService chatService;

    @GetMapping
    public ApiResponse<ChatApiResponse.RoomList> rooms(@AuthenticationPrincipal CustomUserDetails actor) {
        return ApiResponse.ok(chatService.listRooms(actor.getUser()));
    }

    @GetMapping("/{roomId}/messages")
    public ApiResponse<ChatApiResponse.MessagePage> messages(@AuthenticationPrincipal CustomUserDetails actor,
            @PathVariable Long roomId, @RequestParam(required = false) String before, @RequestParam(defaultValue = "30") int size) {
        return ApiResponse.ok(chatService.messagePage(actor.getUser(), roomId, before, size));
    }

    @PostMapping("/{roomId}/messages")
    public ResponseEntity<ApiResponse<ChatApiResponse.SentMessage>> send(@AuthenticationPrincipal CustomUserDetails actor,
            @PathVariable Long roomId, @RequestBody ChatMessageCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(chatService.send(actor.getUser(), roomId, request)));
    }

    @PostMapping("/direct")
    public ApiResponse<ChatApiResponse.DirectRoom> direct(@AuthenticationPrincipal CustomUserDetails actor,
            @RequestBody DirectChatCreateRequest request) {
        return ApiResponse.ok(chatService.openDirectInfo(actor.getUser(), request.projectId()));
    }
}
