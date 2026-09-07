package com.F_A_T.F_A_T.domain.chat.repository;

import com.F_A_T.F_A_T.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT c FROM ChatRoom c WHERE c.project.project_id = :projectId")
    Optional<ChatRoom> findByProjectId(@Param("projectId") Long projectId);
}