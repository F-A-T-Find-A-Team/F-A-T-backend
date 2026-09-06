package com.F_A_T.F_A_T.domain.chat.entity;

import com.F_A_T.F_A_T.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_message")
@Getter
@NoArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_message_id")
    private Long chat_message_id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chat_room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(name = "message_content", columnDefinition = "TEXT", nullable = false)
    private String message_content;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sent_at;

    @Builder
    public ChatMessage(ChatRoom chat_room, User sender, String message_content) {
        this.chat_room = chat_room;
        this.sender = sender;
        this.message_content = message_content;
        this.sent_at = LocalDateTime.now();
    }
}