package com.F_A_T.F_A_T.domain.chat.repository;

import com.F_A_T.F_A_T.domain.chat.entity.ChatParticipant;
import com.F_A_T.F_A_T.domain.chat.entity.ChatRoom;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {
    @Query("select count(p) > 0 from ChatParticipant p where p.room.chat_room_id = :roomId and p.user.user_id = :userId")
    boolean existsParticipant(@Param("roomId") Long roomId, @Param("userId") Long userId);

    @Query("select p.room from ChatParticipant p where p.user.user_id = :userId order by p.room.chat_room_id")
    List<ChatRoom> findRooms(@Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ChatParticipant p where p.room.chat_room_id = :roomId and p.user.user_id = :userId")
    java.util.Optional<ChatParticipant> findParticipant(@Param("roomId") Long roomId, @Param("userId") Long userId);
}
