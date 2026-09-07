package com.F_A_T.F_A_T.domain.chat.repository;

import com.F_A_T.F_A_T.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT m FROM ChatMessage m WHERE m.chat_room.chat_room_id = :chatRoomId ORDER BY m.sent_at ASC")
    List<ChatMessage> findByChatRoomId(@Param("chatRoomId") Long chatRoomId);
}