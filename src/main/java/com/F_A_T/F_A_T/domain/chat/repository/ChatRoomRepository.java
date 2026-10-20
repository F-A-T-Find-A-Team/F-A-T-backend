package com.F_A_T.F_A_T.domain.chat.repository;

import com.F_A_T.F_A_T.domain.chat.entity.ChatRoom;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT c FROM ChatRoom c WHERE c.project.project_id = :projectId AND c.roomType = com.F_A_T.F_A_T.domain.chat.entity.ChatRoomType.TEAM")
    Optional<ChatRoom> findByProjectId(@Param("projectId") Long projectId);

    @Query("select c from ChatRoom c where c.project.project_id = :projectId and c.requester.user_id = :userId and c.roomType = com.F_A_T.F_A_T.domain.chat.entity.ChatRoomType.DIRECT")
    Optional<ChatRoom> findDirect(@Param("projectId") Long projectId, @Param("userId") Long userId);
}
