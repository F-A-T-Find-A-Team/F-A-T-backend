package com.F_A_T.F_A_T.api;

import com.F_A_T.F_A_T.domain.chat.dto.request.ChatMessageCreateRequest;
import com.F_A_T.F_A_T.domain.chat.entity.*;
import com.F_A_T.F_A_T.domain.chat.repository.*;
import com.F_A_T.F_A_T.domain.chat.service.ChatService;
import com.F_A_T.F_A_T.domain.feedback.dto.request.FeedbackSubmissionRequest;
import com.F_A_T.F_A_T.domain.feedback.entity.Feedback;
import com.F_A_T.F_A_T.domain.feedback.repository.FeedbackRepository;
import com.F_A_T.F_A_T.domain.feedback.service.FeedbackService;
import com.F_A_T.F_A_T.domain.project.entity.*;
import com.F_A_T.F_A_T.domain.project.repository.*;
import com.F_A_T.F_A_T.domain.user.entity.User;
import com.F_A_T.F_A_T.domain.user.repository.UserRepository;
import com.F_A_T.F_A_T.global.exception.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatFeedbackApiTests {
    @Mock ChatRoomRepository rooms;
    @Mock ChatMessageRepository messages;
    @Mock ChatParticipantRepository participants;
    @Mock ProjectRepository projects;
    @Mock ProjectMemberRepository members;
    @Mock FeedbackRepository feedbacks;
    @Mock UserRepository users;
    ChatService chat;
    FeedbackService feedback;
    User pm, member, outsider;
    Project project;
    ChatRoom room;

    @BeforeEach void setup() {
        chat = new ChatService(rooms, messages, participants, projects);
        feedback = new FeedbackService(feedbacks, projects, users, members);
        pm = user(1L); member = user(2L); outsider = user(3L);
        project = Project.builder().pm(pm).project_title("Team").project_description("Description")
                .project_deadline(LocalDate.now().plusDays(10)).recruit_limit(3).build();
        ReflectionTestUtils.setField(project, "project_id", 10L);
        room = ChatRoom.builder().project(project).build();
        ReflectionTestUtils.setField(room, "chat_room_id", 20L);
    }

    User user(long id) {
        User user = User.builder().user_name("User " + id).build();
        user.setUser_id(id);
        return user;
    }

    ChatMessage message(long id) {
        ChatMessage message = ChatMessage.builder().chat_room(room).sender(pm).message_content("Message " + id).build();
        ReflectionTestUtils.setField(message, "chat_message_id", id);
        return message;
    }

    void allowChat(User user) {
        when(rooms.findById(20L)).thenReturn(Optional.of(room));
        when(participants.findParticipant(20L, user.getUser_id())).thenReturn(Optional.of(new ChatParticipant(room, user)));
    }

    void completedProject() {
        ReflectionTestUtils.setField(project, "project_status", ProjectStatus.COMPLETED);
        when(projects.findForUpdate(10L)).thenReturn(Optional.of(project));
    }

    void assertCode(ErrorCode code, org.junit.jupiter.api.function.Executable executable) {
        assertEquals(code, assertThrows(ApiException.class, executable).getErrorCode());
    }

    @Test void cursorPageReturnsChronologicalMessagesAndExclusiveNextCursor() {
        allowChat(member);
        when(messages.findBefore(eq(20L), isNull(), any(Pageable.class)))
                .thenReturn(List.of(message(103), message(102), message(101)));
        var response = chat.messagePage(member, 20L, null, 2);
        assertTrue(response.hasMore());
        assertEquals("message_102", response.nextCursor());
        assertEquals(List.of("message_102", "message_103"), response.items().stream().map(item -> item.id()).toList());
        assertEquals("User 1", response.items().get(0).sender().nickname());
        assertNotNull(response.items().get(0).createdAt());
    }

    @Test void cursorCannotReadFromAnotherRoom() {
        allowChat(member);
        ChatRoom different = ChatRoom.builder().project(project).build();
        ReflectionTestUtils.setField(different, "chat_room_id", 999L);
        ChatMessage cursor = ChatMessage.builder().chat_room(different).sender(pm).message_content("other room").build();
        when(messages.findById(103L)).thenReturn(Optional.of(cursor));
        assertCode(ErrorCode.INVALID_MESSAGE_CURSOR, () -> chat.messagePage(member, 20L, "message_103", 30));
        verify(messages, never()).findBefore(anyLong(), any(), any());
    }

    @Test void malformedAndMissingCursorAreRejected() {
        allowChat(member);
        assertCode(ErrorCode.INVALID_MESSAGE_CURSOR, () -> chat.messagePage(member, 20L, "103", 30));
        when(messages.findById(103L)).thenReturn(Optional.empty());
        assertCode(ErrorCode.INVALID_MESSAGE_CURSOR, () -> chat.messagePage(member, 20L, "message_103", 30));
    }

    @Test void messagePaginationIsBounded() {
        assertCode(ErrorCode.INVALID_MESSAGE_CURSOR, () -> chat.messagePage(member, 20L, null, 0));
        assertCode(ErrorCode.INVALID_MESSAGE_CURSOR, () -> chat.messagePage(member, 20L, null, 101));
        verifyNoInteractions(rooms, messages, participants);
    }

    @Test void missingRoomIsNotReportedAsMissingMembership() {
        when(rooms.findById(20L)).thenReturn(Optional.empty());
        assertCode(ErrorCode.CHAT_ROOM_NOT_FOUND, () -> chat.send(member, 20L, new ChatMessageCreateRequest("hello")));
        verifyNoInteractions(participants);
    }

    @Test void nonParticipantCannotSend() {
        when(rooms.findById(20L)).thenReturn(Optional.of(room));
        when(participants.findParticipant(20L, 3L)).thenReturn(Optional.empty());
        assertCode(ErrorCode.NOT_ROOM_PARTICIPANT, () -> chat.send(outsider, 20L, new ChatMessageCreateRequest("hello")));
        verifyNoInteractions(messages);
    }

    @Test void duplicateClientMessageIsNotWrittenTwice() {
        allowChat(member);
        when(messages.existsClientMessage(20L, 2L, "request-id")).thenReturn(true);
        assertCode(ErrorCode.DUPLICATE_MESSAGE,
                () -> chat.send(member, 20L, new ChatMessageCreateRequest("TEXT", "hello", "request-id")));
        verify(messages, never()).saveAndFlush(any());
    }

    @Test void emptyMessageIsAControlledBusinessError() {
        allowChat(member);
        assertCode(ErrorCode.EMPTY_MESSAGE, () -> chat.send(member, 20L, new ChatMessageCreateRequest(" \n ")));
        verify(messages, never()).saveAndFlush(any());
    }

    @Test void bulkReviewValidationFinishesBeforeAnyWrite() {
        completedProject();
        when(members.existsMember(10L, 2L)).thenReturn(true);
        when(users.findById(2L)).thenReturn(Optional.of(member));
        var request = new FeedbackSubmissionRequest(List.of(
                new FeedbackSubmissionRequest.Review(2L, 5, "valid"),
                new FeedbackSubmissionRequest.Review(3L, 6, "invalid")));
        assertCode(ErrorCode.INVALID_FEEDBACK_DATA, () -> feedback.submit(pm, 10L, request));
        verify(feedbacks, never()).save(any());
        verify(feedbacks, never()).flush();
    }

    @Test void duplicateMemberWithinBatchIsNotPartiallyWritten() {
        completedProject();
        when(members.existsMember(10L, 2L)).thenReturn(true);
        when(users.findById(2L)).thenReturn(Optional.of(member));
        var request = new FeedbackSubmissionRequest(List.of(
                new FeedbackSubmissionRequest.Review(2L, 5, "first"),
                new FeedbackSubmissionRequest.Review(2L, 4, "duplicate")));
        assertCode(ErrorCode.INVALID_FEEDBACK_DATA, () -> feedback.submit(pm, 10L, request));
        verify(feedbacks, never()).save(any());
    }

    @Test void alreadyReviewedMemberProducesConflictBeforeWrite() {
        completedProject();
        when(members.existsMember(10L, 2L)).thenReturn(true);
        when(feedbacks.existsReview(10L, 1L, 2L)).thenReturn(true);
        assertCode(ErrorCode.ALREADY_REVIEWED, () -> feedback.submit(pm, 10L,
                new FeedbackSubmissionRequest(List.of(new FeedbackSubmissionRequest.Review(2L, 5, "again")))));
        verify(feedbacks, never()).save(any());
    }

    @Test void feedbackRequiresPmAndCompletedProject() {
        when(projects.findForUpdate(10L)).thenReturn(Optional.of(project));
        var request = new FeedbackSubmissionRequest(List.of(new FeedbackSubmissionRequest.Review(2L, 5, "review")));
        assertCode(ErrorCode.NOT_PROJECT_PM, () -> feedback.submit(member, 10L, request));
        assertCode(ErrorCode.PROJECT_NOT_COMPLETED, () -> feedback.submit(pm, 10L, request));
        verifyNoInteractions(feedbacks, members);
    }

    @Test void feedbackStatusUsesProjectStageAndExcludesPmFromPending() {
        ReflectionTestUtils.setField(project, "project_status", ProjectStatus.COMPLETED);
        when(projects.findById(10L)).thenReturn(Optional.of(project));
        when(members.findMembers(10L)).thenReturn(List.of(pm, member, outsider));
        when(feedbacks.findEvaluatedMemberIds(10L)).thenReturn(List.of(2L));
        var status = feedback.status(pm, 10L);
        assertTrue(status.isCompleted());
        assertEquals(List.of(2L), status.reviewedUserIds());
        assertEquals(List.of(3L), status.pendingUserIds());
    }

    @Test void receivedFeedbackUsesGlobalAverageAndPageMetadata() {
        when(users.existsById(2L)).thenReturn(true);
        when(feedbacks.averageRating(2L)).thenReturn(4.75);
        Feedback review = Feedback.builder().project(project).writer(pm).target(member).rating(5).feedback_content("great").build();
        ReflectionTestUtils.setField(review, "feedback_id", 50L);
        when(feedbacks.findReceivedPage(eq(2L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(review), PageRequest.of(0, 1), 12));
        var response = feedback.received(member, 0, 1);
        assertEquals(4.8, response.averageRating());
        assertEquals(12, response.totalReviews());
        assertEquals(12, response.totalElements());
        assertEquals("great", response.items().get(0).comment());
    }
}
