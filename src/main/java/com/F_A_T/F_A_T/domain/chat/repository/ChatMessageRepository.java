package com.F_A_T.F_A_T.domain.chat.repository;

import com.F_A_T.F_A_T.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByChat_room_Chat_room_idOrderBySent_atAsc(Long chatRoomId);
}