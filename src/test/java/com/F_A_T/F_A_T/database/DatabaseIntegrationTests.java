package com.F_A_T.F_A_T.database;

import com.F_A_T.F_A_T.domain.application.dto.request.*;
import com.F_A_T.F_A_T.domain.application.entity.ApplicationStatus;
import com.F_A_T.F_A_T.domain.application.repository.ProjectApplicationRepository;
import com.F_A_T.F_A_T.domain.application.service.ProjectApplicationService;
import com.F_A_T.F_A_T.domain.chat.dto.request.ChatMessageCreateRequest;
import com.F_A_T.F_A_T.domain.chat.repository.*;
import com.F_A_T.F_A_T.domain.chat.service.ChatService;
import com.F_A_T.F_A_T.domain.feedback.dto.request.FeedbackBulkCreateRequest;
import com.F_A_T.F_A_T.domain.feedback.service.FeedbackService;
import com.F_A_T.F_A_T.domain.project.dto.request.*;
import com.F_A_T.F_A_T.domain.project.entity.ProjectStatus;
import com.F_A_T.F_A_T.domain.project.repository.*;
import com.F_A_T.F_A_T.domain.project.service.ProjectService;
import com.F_A_T.F_A_T.domain.user.dto.request.SignupRequest;
import com.F_A_T.F_A_T.domain.user.entity.User;
import com.F_A_T.F_A_T.domain.user.repository.UserRepository;
import com.F_A_T.F_A_T.domain.user.service.UserService;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=validate"})
@Testcontainers
@Transactional
class DatabaseIntegrationTests {
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", MYSQL::getJdbcUrl);
        r.add("spring.datasource.username", MYSQL::getUsername);
        r.add("spring.datasource.password", MYSQL::getPassword);
    }
    @Autowired UserRepository users;
    @Autowired UserService userService;
    @Autowired ProjectService projects;
    @Autowired ProjectRepository projectRepository;
    @Autowired ProjectMemberRepository members;
    @Autowired ProjectApplicationService applications;
    @Autowired ProjectApplicationRepository applicationRepository;
    @Autowired ChatService chats;
    @Autowired ChatRoomRepository rooms;
    @Autowired ChatParticipantRepository participants;
    @Autowired ChatMessageRepository messages;
    @Autowired FeedbackService feedback;
    @Autowired com.F_A_T.F_A_T.domain.feedback.repository.FeedbackRepository feedbackRepository;
    @Autowired EntityManager em;
    @Autowired com.F_A_T.F_A_T.domain.mypage.service.MyPageService myPage;
    @Autowired com.F_A_T.F_A_T.domain.auth.repository.EmailVerificationRepository verifications;
    @Autowired com.F_A_T.F_A_T.domain.auth.repository.RefreshTokenRepository tokens;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;

    User user(String email) {
        return users.save(User.builder().user_email(email).user_password("hash")
                .user_name("tester").user_gender("X").user_major("CS")
                .user_student_number(20260001).interest_stacks(List.of("Java")).build());
    }
    Long project(User pm, int capacity) {
        return projects.createProject(pm, new ProjectCreateRequest("project", "description", List.of("CS"),
                List.of("Java"), LocalDate.now().plusDays(30), capacity));
    }
    Long apply(User user, Long id) {
        return applications.apply(user, id, new ApplicationCreateRequest("hello", "Backend"));
    }
    void accept(User pm, Long application) {
        applications.updateStatus(pm, application, new ApplicationStatusUpdateRequest(ApplicationStatus.ACCEPTED));
    }

    @Test void signupLoginAndEmailQueriesWorkWithNamedParameters() {
        var id = userService.signup(new SignupRequest("signup@test.invalid", "password123!", "X", "CS", List.of("Java"), 20260001, "tester"));
        assertTrue(users.existsByUserEmail("signup@test.invalid"));
        assertEquals(id, userService.login("signup@test.invalid", "password123!").getUser_id());
        assertThrows(IllegalArgumentException.class, () -> userService.login("signup@test.invalid", "incorrect"));
        assertTrue(users.findByUserEmail("missing@test.invalid").isEmpty());
    }

    @Test void acceptanceCreatesMemberAndParticipantOnceAndAllowsMessages() {
        User pm = user("pm@test.invalid"), member = user("member@test.invalid");
        Long id = project(pm, 3), application = apply(member, id);
        accept(pm, application); accept(pm, application);
        em.flush(); em.clear();
        assertEquals(2, members.countMembers(id));
        var room = rooms.findByProjectId(id).orElseThrow();
        assertTrue(participants.existsParticipant(room.getChat_room_id(), member.getUser_id()));
        var message = chats.sendMessage(member, room.getChat_room_id(), new ChatMessageCreateRequest("hello"));
        em.flush(); em.clear();
        assertEquals("hello", messages.findById(message).orElseThrow().getMessage_content());
        assertEquals(1, chats.getMessages(member, room.getChat_room_id()).size());
        assertEquals(1, chats.getMyChatRooms(member).size());
        assertEquals(2, projects.getMembers(id).size());
    }

    @Test void pendingAndRejectedApplicationsDoNotCreateMembers() {
        User pm=user("pm@test.invalid"), applicant=user("a@test.invalid");
        Long id=project(pm,3), application=apply(applicant,id);
        applications.updateStatus(pm, application, new ApplicationStatusUpdateRequest(ApplicationStatus.REJECTED));
        assertEquals(1, members.countMembers(id));
        assertFalse(members.existsMember(id,applicant.getUser_id()));
    }

    @Test void outsidersCannotReadOrSendChat() {
        User pm=user("pm@test.invalid"), outsider=user("outsider@test.invalid");
        Long id=project(pm,3), roomId=rooms.findByProjectId(id).orElseThrow().getChat_room_id();
        assertTrue(chats.getMyChatRooms(outsider).isEmpty());
        assertThrows(AccessDeniedException.class, () -> chats.getMessages(outsider,roomId));
        assertThrows(AccessDeniedException.class, () -> chats.sendMessage(outsider,roomId,new ChatMessageCreateRequest("x")));
    }

    @Test void onlyPmCanDecideApplicationsAndProjectStatus() {
        User pm=user("pm@test.invalid"), outsider=user("outsider@test.invalid");
        Long id=project(pm,3), application=apply(outsider,id);
        assertThrows(AccessDeniedException.class, () -> accept(outsider,application));
        assertThrows(AccessDeniedException.class, () -> projects.updateStatus(outsider,id,new ProjectStatusUpdateRequest(ProjectStatus.COMPLETED)));
    }

    @Test void capacityIncludesPmAndAcceptedMembershipCannotBeSilentlyRevoked() {
        User pm=user("pm@test.invalid"), a=user("a@test.invalid"), b=user("b@test.invalid");
        Long id=project(pm,2), appA=apply(a,id), appB=apply(b,id);
        accept(pm,appA);
        assertThrows(IllegalArgumentException.class, () -> accept(pm,appB));
        assertThrows(IllegalArgumentException.class, () -> applications.updateStatus(pm,appA,new ApplicationStatusUpdateRequest(ApplicationStatus.REJECTED)));
        assertEquals(2,members.countMembers(id));
    }

    @Test void completedProjectFeedbackPersistsForMembers() {
        User pm=user("pm@test.invalid"), a=user("a@test.invalid");
        Long id=project(pm,3); accept(pm,apply(a,id));
        projects.updateStatus(pm,id,new ProjectStatusUpdateRequest(ProjectStatus.COMPLETED));
        var ids=feedback.createFeedbacks(pm,id,new FeedbackBulkCreateRequest(List.of(new FeedbackBulkCreateRequest.FeedbackItem(a.getUser_id(),5,"thanks"))));
        em.flush(); em.clear();
        assertEquals(ProjectStatus.COMPLETED,projectRepository.findById(id).orElseThrow().getProject_status());
        assertEquals(1,ids.size()); assertEquals(1,feedback.getFeedbacks(id).size());
        assertEquals(5.0,feedbackRepository.averageRating(a.getUser_id()));
        assertEquals(1,feedbackRepository.findReceived(a.getUser_id()).size());
        assertEquals(List.of(a.getUser_id()),feedbackRepository.findEvaluatedMemberIds(id));
        assertEquals(1,members.findProjects(a.getUser_id()).size());
    }

    @Test void feedbackRejectsUnfinishedProjectsSelfAndNonMembers() {
        User pm=user("pm@test.invalid"), outsider=user("outsider@test.invalid");
        Long id=project(pm,3);
        var request=new FeedbackBulkCreateRequest(List.of(new FeedbackBulkCreateRequest.FeedbackItem(outsider.getUser_id(),5,"thanks")));
        assertThrows(IllegalArgumentException.class, () -> feedback.createFeedbacks(pm,id,request));
        projects.updateStatus(pm,id,new ProjectStatusUpdateRequest(ProjectStatus.COMPLETED));
        assertThrows(IllegalArgumentException.class, () -> feedback.createFeedbacks(pm,id,request));
        assertThrows(AccessDeniedException.class, () -> feedback.createFeedbacks(outsider,id,request));
        assertThrows(IllegalArgumentException.class, () -> feedback.createFeedbacks(pm,id,new FeedbackBulkCreateRequest(List.of(new FeedbackBulkCreateRequest.FeedbackItem(pm.getUser_id(),5,"self")))));
    }

    @Test void directRoomsAreIdempotentPrivateAndSeparateFromTeamRooms() {
        User pm=user("pm@test.invalid"), requester=user("requester@test.invalid"), outsider=user("outsider@test.invalid");
        Long id=project(pm,3), teamRoom=rooms.findByProjectId(id).orElseThrow().getChat_room_id();
        Long direct=chats.openDirect(requester,id);
        assertEquals(direct,chats.openDirect(requester,id));
        assertNotEquals(teamRoom,direct);
        assertTrue(participants.existsParticipant(direct,pm.getUser_id()));
        assertTrue(participants.existsParticipant(direct,requester.getUser_id()));
        assertFalse(participants.existsParticipant(teamRoom,requester.getUser_id()));
        assertThrows(AccessDeniedException.class, () -> chats.getMessages(outsider,direct));
        assertThrows(IllegalArgumentException.class, () -> chats.openDirect(pm,id));
        Long otherDirect=chats.openDirect(outsider,id);
        assertNotEquals(direct,otherDirect);
    }

    @Test void readCursorCountsOtherSendersAndOnlyAdvancesWithinTheRoom() {
        User pm=user("pm@test.invalid"), a=user("a@test.invalid");
        Long id=project(pm,3); accept(pm,apply(a,id));
        Long room=rooms.findByProjectId(id).orElseThrow().getChat_room_id();
        Long first=chats.sendMessage(pm,room,new ChatMessageCreateRequest("one"));
        chats.sendMessage(a,room,new ChatMessageCreateRequest("own"));
        Long third=chats.sendMessage(pm,room,new ChatMessageCreateRequest("three"));
        em.flush();
        assertEquals(2,messages.countUnread(room,a.getUser_id()));
        assertEquals(2,chats.getMyChatRooms(a).get(0).unreadCount());
        assertEquals(3,chats.getMessages(a,room).size());
        em.flush();
        assertEquals(0,messages.countUnread(room,a.getUser_id()));
        var participant=participants.findParticipant(room,a.getUser_id()).orElseThrow();
        participant.markRead(messages.findById(first).orElseThrow());
        assertEquals(third,participant.getLastReadMessage().getChat_message_id());
        Long direct=chats.openDirect(a,id);
        Long different=chats.sendMessage(pm,direct,new ChatMessageCreateRequest("different"));
        assertThrows(IllegalArgumentException.class, () -> participant.markRead(messages.findById(different).orElseThrow()));
    }

    @Test void pendingCancellationPreservesHistoryAndCannotBeAccepted() {
        User pm=user("pm@test.invalid"), a=user("a@test.invalid");
        Long id=project(pm,3), application=apply(a,id);
        assertThrows(AccessDeniedException.class, () -> applications.cancel(pm,application));
        applications.cancel(a,application);
        em.flush();
        assertEquals(ApplicationStatus.CANCELLED,applicationRepository.findById(application).orElseThrow().getApplication_status());
        assertEquals(1,applicationRepository.findByApplicantId(a.getUser_id()).size());
        assertEquals(1,applicationRepository.findByProjectId(id).size());
        assertThrows(IllegalArgumentException.class, () -> accept(pm,application));
        assertEquals(1,members.countMembers(id));
    }

    @Test void signupTokenConsumptionAndRefreshRevocationAreSingleUse() {
        var now=java.time.LocalDateTime.now();
        var verification=verifications.save(new com.F_A_T.F_A_T.domain.auth.entity.EmailVerification("verified@test.invalid","a".repeat(64),now.plusMinutes(5)));
        verifications.findForUpdate(verification.getId()).orElseThrow().markVerified("b".repeat(64),now.plusMinutes(10),now);
        assertEquals(1,verifications.consumeSignupToken("b".repeat(64),now));
        assertEquals(0,verifications.consumeSignupToken("b".repeat(64),now));
        User user=user("token@test.invalid");
        tokens.save(new com.F_A_T.F_A_T.domain.auth.entity.RefreshToken(user,"c".repeat(64),now,now.plusDays(7)));
        tokens.save(new com.F_A_T.F_A_T.domain.auth.entity.RefreshToken(user,"d".repeat(64),now.minusDays(10),now.minusDays(1)));
        em.flush();
        assertTrue(tokens.findActive("c".repeat(64),now).isPresent());
        assertTrue(tokens.findActive("d".repeat(64),now).isEmpty());
        assertEquals(0,tokens.revokeActive("d".repeat(64),now));
        assertEquals(1,tokens.revokeActive("c".repeat(64),now));
        assertEquals(0,tokens.revokeActive("c".repeat(64),now));
        assertTrue(tokens.findActive("c".repeat(64),now).isEmpty());
    }

    @Test void expiredSignupTokenCannotBeConsumed() {
        var now=java.time.LocalDateTime.now();
        var verification=new com.F_A_T.F_A_T.domain.auth.entity.EmailVerification("expired@test.invalid","e".repeat(64),now.plusMinutes(5));
        verification.markVerified("f".repeat(64),now.minusMinutes(1),now);
        verifications.saveAndFlush(verification);
        assertEquals(0,verifications.consumeSignupToken("f".repeat(64),now));
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void concurrentAcceptancesCannotOverfillTheProject() throws Exception {
        var tx=new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        Object[] setup=tx.execute(status -> {
            User pm=user("race-pm@test.invalid"), a=user("race-a@test.invalid"), b=user("race-b@test.invalid");
            Long id=project(pm,2);
            return new Object[]{pm,id,apply(a,id),apply(b,id)};
        });
        var start=new java.util.concurrent.CountDownLatch(1);
        var ready=new java.util.concurrent.CountDownLatch(2);
        var executor=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var tasks=new java.util.ArrayList<java.util.concurrent.Future<Boolean>>();
            for(int i=2;i<4;i++) {
                Long application=(Long) setup[i];
                tasks.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(15,java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("start timed out");
                    try { accept((User)setup[0],application); return true; }
                    catch (IllegalArgumentException expected) { return false; }
                }));
            }
            assertTrue(ready.await(15,java.util.concurrent.TimeUnit.SECONDS)); start.countDown();
            int successes=0;
            for(var task:tasks) if(task.get(30,java.util.concurrent.TimeUnit.SECONDS)) successes++;
            assertEquals(1,successes);
            int memberCount=tx.execute(status -> members.countMembers((Long)setup[1]));
            assertEquals(2,memberCount);
        } finally { start.countDown(); executor.shutdownNow(); }
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void responseCollectionsRemainReadableAfterServiceTransactionsEnd() {
        var tx=new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        User pm=tx.execute(status -> user("detached-profile@test.invalid"));
        Long id=project(pm,3);
        var detail=projects.getProject(id);
        var list=projects.getProjectList();
        var profile=myPage.getMyPage(pm);
        assertEquals(List.of("Java"),detail.requiredStacks());
        assertEquals(List.of("CS"),detail.requiredMajors());
        assertEquals(List.of("Java"),list.stream().filter(item -> item.projectId().equals(id)).findFirst().orElseThrow().requiredStacks());
        assertEquals(List.of("Java"),profile.interestStacks());
    }
}
