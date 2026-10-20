package com.F_A_T.F_A_T.domain.chat.service;

import com.F_A_T.F_A_T.domain.chat.dto.request.ChatMessageCreateRequest;
import com.F_A_T.F_A_T.domain.chat.dto.response.ChatMessageResponse;
import com.F_A_T.F_A_T.domain.chat.dto.response.ChatRoomResponse;
import com.F_A_T.F_A_T.domain.chat.entity.ChatMessage;
import com.F_A_T.F_A_T.domain.chat.entity.ChatParticipant;
import com.F_A_T.F_A_T.domain.chat.entity.ChatRoom;
import com.F_A_T.F_A_T.domain.chat.repository.ChatMessageRepository;
import com.F_A_T.F_A_T.domain.chat.repository.ChatParticipantRepository;
import com.F_A_T.F_A_T.domain.chat.repository.ChatRoomRepository;
import com.F_A_T.F_A_T.domain.project.repository.ProjectRepository;
import com.F_A_T.F_A_T.domain.user.entity.User;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatParticipantRepository participantRepository;
    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getMyChatRooms(User actor) {
        return participantRepository.findRooms(actor.getUser_id()).stream()
                .map(room -> ChatRoomResponse.from(room,
                        chatMessageRepository.findLatest(room.getChat_room_id(), PageRequest.of(0, 1)).stream().findFirst().orElse(null),
                        chatMessageRepository.countUnread(room.getChat_room_id(), actor.getUser_id())))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<ChatMessageResponse> getMessages(User actor, Long chatRoomId) {
        requireParticipant(actor, chatRoomId);
        var participant = participantRepository.findParticipant(chatRoomId, actor.getUser_id()).orElseThrow();
        var messages = chatMessageRepository.findByChatRoomId(chatRoomId);
        messages.stream().max(java.util.Comparator.comparing(ChatMessage::getChat_message_id))
                .ifPresent(participant::markRead);
        return messages.stream().map(ChatMessageResponse::from).toList();
    }

    @Transactional
    public Long sendMessage(User sender, Long chatRoomId, ChatMessageCreateRequest request) {
        requireParticipant(sender, chatRoomId);
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));

        ChatMessage message = ChatMessage.builder()
                .chat_room(chatRoom)
                .sender(sender)
                .message_content(request.content())
                .build();

        return chatMessageRepository.save(message).getChat_message_id();
    }
    private void requireParticipant(User actor, Long roomId) {
        if (!participantRepository.existsParticipant(roomId, actor.getUser_id())) {
            throw new AccessDeniedException("채팅방 참여자만 접근할 수 있습니다.");
        }
    }

    @Transactional
    public Long openDirect(User requester, Long projectId) {
        var project = projectRepository.findForUpdate(projectId).orElseThrow();
        if (project.getPm().getUser_id().equals(requester.getUser_id())) {
            throw new IllegalArgumentException("자신과 커피챗을 시작할 수 없습니다.");
        }
        return chatRoomRepository.findDirect(projectId, requester.getUser_id()).orElseGet(() -> {
            var room = chatRoomRepository.save(ChatRoom.direct(project, requester));
            participantRepository.save(new ChatParticipant(room, project.getPm()));
            participantRepository.save(new ChatParticipant(room, requester));
            return room;
        }).getChat_room_id();
    }
}
