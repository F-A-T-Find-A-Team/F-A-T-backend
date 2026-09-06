package com.F_A_T.F_A_T.domain.chat.dto.response;

import com.F_A_T.F_A_T.domain.chat.entity.ChatMessage;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long chatMessageId,
        Long senderId,
        String content,
        LocalDateTime sentAt
) {
    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getChat_message_id(),
                message.getSender().getUser_id(),
                message.getMessage_content(),
                message.getSent_at()
        );
    }
}