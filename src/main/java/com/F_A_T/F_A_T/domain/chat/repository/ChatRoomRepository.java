package com.F_A_T.F_A_T.domain.chat.repository;

import com.F_A_T.F_A_T.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByProject_Project_id(Long projectId);
}