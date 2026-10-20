package com.F_A_T.F_A_T.domain.chat.repository;

import com.F_A_T.F_A_T.domain.chat.entity.ChatMessage;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT m FROM ChatMessage m WHERE m.chat_room.chat_room_id = :chatRoomId ORDER BY m.sent_at ASC, m.chat_message_id ASC")
    List<ChatMessage> findByChatRoomId(@Param("chatRoomId") Long chatRoomId);

    @Query("select count(m) from ChatParticipant p left join p.lastReadMessage lastRead, ChatMessage m where p.room.chat_room_id = :roomId and p.user.user_id = :userId and m.chat_room = p.room and m.sender.user_id <> :userId and m.chat_message_id > coalesce(lastRead.chat_message_id, 0)")
    long countUnread(@Param("roomId") Long roomId, @Param("userId") Long userId);

    @Query("select m from ChatMessage m where m.chat_room.chat_room_id = :roomId order by m.chat_message_id desc")
    List<ChatMessage> findLatest(@Param("roomId") Long roomId, Pageable page);
}
