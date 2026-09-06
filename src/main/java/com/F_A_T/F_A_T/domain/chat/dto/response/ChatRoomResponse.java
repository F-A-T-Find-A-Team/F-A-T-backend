package com.F_A_T.F_A_T.domain.chat.dto.response;

import com.F_A_T.F_A_T.domain.chat.entity.ChatRoom;

public record ChatRoomResponse(
        Long chatRoomId,
        Long projectId,
        String projectTitle
) {
    public static ChatRoomResponse from(ChatRoom chatRoom) {
        return new ChatRoomResponse(
                chatRoom.getChat_room_id(),
                chatRoom.getProject().getProject_id(),
                chatRoom.getProject().getProject_title()
        );
    }
}