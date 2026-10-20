package com.F_A_T.F_A_T.domain.chat.dto.response;

import com.F_A_T.F_A_T.domain.chat.entity.*;

public record ChatRoomResponse(
        Long chatRoomId,
        Long projectId,
        String projectTitle,
        ChatRoomType roomType,
        ChatMessageResponse latestMessage,
        long unreadCount
) {
    public static ChatRoomResponse from(ChatRoom room, ChatMessage latest, long unreadCount) {
        return new ChatRoomResponse(room.getChat_room_id(), room.getProject().getProject_id(),
                room.getProject().getProject_title(), room.getRoomType(),
                latest == null ? null : ChatMessageResponse.from(latest), unreadCount);
    }
}
