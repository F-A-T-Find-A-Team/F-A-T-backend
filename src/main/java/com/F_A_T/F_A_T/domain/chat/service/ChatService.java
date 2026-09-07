package com.F_A_T.F_A_T.domain.chat.service;

import com.F_A_T.F_A_T.domain.chat.dto.request.ChatMessageCreateRequest;
import com.F_A_T.F_A_T.domain.chat.dto.response.ChatMessageResponse;
import com.F_A_T.F_A_T.domain.chat.dto.response.ChatRoomResponse;
import com.F_A_T.F_A_T.domain.chat.entity.ChatMessage;
import com.F_A_T.F_A_T.domain.chat.entity.ChatRoom;
import com.F_A_T.F_A_T.domain.chat.repository.ChatMessageRepository;
import com.F_A_T.F_A_T.domain.chat.repository.ChatRoomRepository;
import com.F_A_T.F_A_T.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getMyChatRooms() {
        return chatRoomRepository.findAll().stream()
                .map(ChatRoomResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long chatRoomId) {
        return chatMessageRepository.findByChatRoomId(chatRoomId).stream()
                .map(ChatMessageResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public Long sendMessage(User sender, Long chatRoomId, ChatMessageCreateRequest request) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));

        ChatMessage message = ChatMessage.builder()
                .chat_room(chatRoom)
                .sender(sender)
                .message_content(request.content())
                .build();

        return chatMessageRepository.save(message).getChat_message_id();
    }
}