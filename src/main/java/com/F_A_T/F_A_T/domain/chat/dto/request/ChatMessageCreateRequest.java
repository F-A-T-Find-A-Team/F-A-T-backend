package com.F_A_T.F_A_T.domain.chat.dto.request;

/** clientMessageId is optional for older clients; new clients should send a UUID. */
public record ChatMessageCreateRequest(String type, String content, String clientMessageId) {
    public ChatMessageCreateRequest(String content) { this("TEXT", content, null); }
}
