package com.F_A_T.F_A_T.domain.chat.dto.response;

import com.F_A_T.F_A_T.domain.chat.entity.ChatRoomType;
import java.time.Instant;
import java.util.List;

public final class ChatApiResponse {
    private ChatApiResponse() {}
    public record RoomList(List<RoomItem> items) {}
    public record RoomItem(Long id, ChatRoomType type, String name, Long projectId,
                           String lastMessage, Instant lastMessageAt, long unreadCount) {}
    public record DirectRoom(Long roomId, ChatRoomType type, boolean created) {}
    public record Sender(Long id, String nickname) {}
    public record MessageItem(String id, Long roomId, Sender sender, String type, String content, Instant createdAt) {}
    public record MessagePage(List<MessageItem> items, String nextCursor, boolean hasMore) {}
    public record SentMessage(String id, Long roomId, Long senderId, String type, String content, Instant createdAt) {}
}
