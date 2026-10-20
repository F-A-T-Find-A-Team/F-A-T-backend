package com.F_A_T.F_A_T.domain.chat.entity;

import com.F_A_T.F_A_T.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_participants", uniqueConstraints = @UniqueConstraint(name = "uk_chat_participant", columnNames = {"chat_room_id", "user_id"}))
@Getter
@NoArgsConstructor
public class ChatParticipant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_participant_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_read_message_id")
    private ChatMessage lastReadMessage;

    public void markRead(ChatMessage message) {
        if (!room.getChat_room_id().equals(message.getChat_room().getChat_room_id())) {
            throw new IllegalArgumentException("다른 채팅방의 메시지입니다.");
        }
        if (lastReadMessage == null || lastReadMessage.getChat_message_id() < message.getChat_message_id()) {
            lastReadMessage = message;
        }
    }

    public ChatParticipant(ChatRoom room, User user) {
        this.room = room;
        this.user = user;
    }
}
