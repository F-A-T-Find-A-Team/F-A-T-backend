package com.F_A_T.F_A_T.domain.chat.service;

import com.F_A_T.F_A_T.domain.chat.dto.request.ChatMessageCreateRequest;
import com.F_A_T.F_A_T.domain.chat.dto.response.*;
import com.F_A_T.F_A_T.domain.chat.entity.*;
import com.F_A_T.F_A_T.domain.chat.repository.*;
import com.F_A_T.F_A_T.domain.project.repository.ProjectRepository;
import com.F_A_T.F_A_T.domain.user.entity.User;
import com.F_A_T.F_A_T.global.exception.ApiException;
import com.F_A_T.F_A_T.global.exception.ErrorCode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatParticipantRepository participantRepository;
    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public ChatApiResponse.RoomList listRooms(User actor) {
        var rooms = participantRepository.findRooms(actor.getUser_id()).stream().map(room -> {
            ChatMessage latest = chatMessageRepository.findLatest(room.getChat_room_id(), PageRequest.of(0, 1))
                    .stream().findFirst().orElse(null);
            String name = room.getProject().getProject_title() + " 팀 채팅방";
            if (room.getRoomType() == ChatRoomType.DIRECT) {
                User other = room.getProject().getPm().getUser_id().equals(actor.getUser_id())
                        ? room.getRequester() : room.getProject().getPm();
                name = other.getNickname() + (other.getUser_id().equals(room.getProject().getPm().getUser_id()) ? " · PM" : "");
            }
            return new ChatApiResponse.RoomItem(room.getChat_room_id(), room.getRoomType(), name,
                    room.getProject().getProject_id(), latest == null ? null : latest.getMessage_content(),
                    latest == null ? null : instant(latest),
                    chatMessageRepository.countUnread(room.getChat_room_id(), actor.getUser_id()));
        }).sorted(Comparator.comparing(ChatApiResponse.RoomItem::lastMessageAt,
                Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(ChatApiResponse.RoomItem::id, Comparator.reverseOrder())).toList();
        return new ChatApiResponse.RoomList(rooms);
    }

    // Retained service adapter for clients predating the documented REST response.
    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getMyChatRooms(User actor) {
        return participantRepository.findRooms(actor.getUser_id()).stream().map(room -> ChatRoomResponse.from(room,
                chatMessageRepository.findLatest(room.getChat_room_id(), PageRequest.of(0, 1)).stream().findFirst().orElse(null),
                chatMessageRepository.countUnread(room.getChat_room_id(), actor.getUser_id()))).toList();
    }

    @Transactional
    public ChatApiResponse.MessagePage messagePage(User actor, Long roomId, String before, int size) {
        if (size < 1 || size > 100) throw new ApiException(ErrorCode.INVALID_MESSAGE_CURSOR);
        ChatRoom room = requireRoom(roomId);
        ChatParticipant participant = requireParticipant(actor, room);
        Long beforeId = null;
        if (before != null) {
            beforeId = parseCursor(before);
            ChatMessage cursor = chatMessageRepository.findById(beforeId)
                    .orElseThrow(() -> new ApiException(ErrorCode.INVALID_MESSAGE_CURSOR));
            if (!cursor.getChat_room().getChat_room_id().equals(roomId)) throw new ApiException(ErrorCode.INVALID_MESSAGE_CURSOR);
        }
        var fetched = chatMessageRepository.findBefore(roomId, beforeId, PageRequest.of(0, size + 1));
        boolean hasMore = fetched.size() > size;
        var selected = new ArrayList<>(fetched.subList(0, Math.min(size, fetched.size())));
        if (!selected.isEmpty()) participant.markRead(selected.get(0));
        String cursor = hasMore ? messageId(selected.get(selected.size() - 1)) : null;
        Collections.reverse(selected);
        return new ChatApiResponse.MessagePage(selected.stream().map(this::messageItem).toList(), cursor, hasMore);
    }

    @Transactional
    public List<ChatMessageResponse> getMessages(User actor, Long roomId) {
        ChatParticipant participant = requireParticipant(actor, requireRoom(roomId));
        var messages = chatMessageRepository.findByChatRoomId(roomId);
        messages.stream().max(Comparator.comparing(ChatMessage::getChat_message_id)).ifPresent(participant::markRead);
        return messages.stream().map(ChatMessageResponse::from).toList();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ChatApiResponse.SentMessage send(User sender, Long roomId, ChatMessageCreateRequest request) {
        ChatMessage message = saveMessage(sender, roomId, request);
        return new ChatApiResponse.SentMessage(messageId(message), roomId, sender.getUser_id(), "TEXT",
                message.getMessage_content(), instant(message));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Long sendMessage(User sender, Long roomId, ChatMessageCreateRequest request) {
        return saveMessage(sender, roomId, request).getChat_message_id();
    }

    private ChatMessage saveMessage(User sender, Long roomId, ChatMessageCreateRequest request) {
        ChatRoom room = requireRoom(roomId);
        // Serialize this participant's sends to make clientMessageId checking safe across retries.
        requireParticipant(sender, room, "메시지 전송 권한이 없습니다.");
        if (request == null || request.content() == null || request.content().isBlank()) throw new ApiException(ErrorCode.EMPTY_MESSAGE);
        if ((request.type() != null && !request.type().equals("TEXT")) || request.content().length() > 10000
                || (request.clientMessageId() != null && (request.clientMessageId().isBlank() || request.clientMessageId().length() > 128))) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        if (request.clientMessageId() != null && chatMessageRepository.existsClientMessage(roomId, sender.getUser_id(), request.clientMessageId())) {
            throw new ApiException(ErrorCode.DUPLICATE_MESSAGE);
        }
        ChatMessage message = ChatMessage.builder().chat_room(room).sender(sender).message_content(request.content()).build();
        message.setClientMessageId(request.clientMessageId());
        return chatMessageRepository.saveAndFlush(message);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ChatApiResponse.DirectRoom openDirectInfo(User requester, Long projectId) {
        if (projectId == null || projectId < 1) throw new ApiException(ErrorCode.INVALID_PROJECT_ID);
        var project = projectRepository.findForUpdate(projectId).orElseThrow(() -> new ApiException(ErrorCode.PROJECT_NOT_FOUND));
        if (project.getPm().getUser_id().equals(requester.getUser_id())) throw new ApiException(ErrorCode.CANNOT_CHAT_WITH_SELF);
        var existing = chatRoomRepository.findDirect(projectId, requester.getUser_id());
        if (existing.isPresent()) return new ChatApiResponse.DirectRoom(existing.get().getChat_room_id(), ChatRoomType.DIRECT, false);
        ChatRoom room = chatRoomRepository.save(ChatRoom.direct(project, requester));
        participantRepository.save(new ChatParticipant(room, project.getPm()));
        participantRepository.save(new ChatParticipant(room, requester));
        return new ChatApiResponse.DirectRoom(room.getChat_room_id(), ChatRoomType.DIRECT, true);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Long openDirect(User requester, Long projectId) { return openDirectInfo(requester, projectId).roomId(); }

    private ChatRoom requireRoom(Long id) {
        if (id == null || id < 1) throw new ApiException(ErrorCode.CHAT_ROOM_NOT_FOUND);
        return chatRoomRepository.findById(id).orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    private ChatParticipant requireParticipant(User actor, ChatRoom room) {
        return requireParticipant(actor, room, "채팅방에 참여하고 있지 않습니다.");
    }

    private ChatParticipant requireParticipant(User actor, ChatRoom room, String message) {
        return participantRepository.findParticipant(room.getChat_room_id(), actor.getUser_id())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_ROOM_PARTICIPANT, message));
    }

    private Long parseCursor(String cursor) {
        if (!cursor.matches("message_[1-9][0-9]*")) throw new ApiException(ErrorCode.INVALID_MESSAGE_CURSOR);
        try { return Long.parseLong(cursor.substring(8)); }
        catch (NumberFormatException e) { throw new ApiException(ErrorCode.INVALID_MESSAGE_CURSOR); }
    }

    private String messageId(ChatMessage message) { return "message_" + message.getChat_message_id(); }
    private Instant instant(ChatMessage message) { return message.getSent_at().toInstant(ZoneOffset.UTC); }
    private ChatApiResponse.MessageItem messageItem(ChatMessage message) {
        return new ChatApiResponse.MessageItem(messageId(message), message.getChat_room().getChat_room_id(),
                new ChatApiResponse.Sender(message.getSender().getUser_id(), message.getSender().getNickname()),
                "TEXT", message.getMessage_content(), instant(message));
    }
}
