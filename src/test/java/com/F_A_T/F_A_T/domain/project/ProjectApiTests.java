package com.F_A_T.F_A_T.domain.project;

import com.F_A_T.F_A_T.domain.application.dto.request.*;
import com.F_A_T.F_A_T.domain.application.entity.ApplicationStatus;
import com.F_A_T.F_A_T.domain.application.service.ProjectApplicationService;
import com.F_A_T.F_A_T.domain.project.dto.request.*;
import com.F_A_T.F_A_T.domain.project.repository.ProjectMemberRepository;
import com.F_A_T.F_A_T.domain.project.service.ProjectService;
import com.F_A_T.F_A_T.domain.user.entity.User;
import com.F_A_T.F_A_T.domain.user.repository.UserRepository;
import com.F_A_T.F_A_T.global.exception.*;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Transactional
class ProjectApiTests {
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", MYSQL::getJdbcUrl);
        r.add("spring.datasource.username", MYSQL::getUsername);
        r.add("spring.datasource.password", MYSQL::getPassword);
    }
    @Autowired UserRepository users;
    @Autowired ProjectService projects;
    @Autowired ProjectApplicationService applications;
    @Autowired ProjectMemberRepository members;

    User user(String email, String major, String stack) {
        return users.save(User.builder().user_email(email).user_password("test-hash")
                .user_name(email).user_gender("OTHER").user_major(major).interest_stacks(List.of(stack))
                .user_student_number(20260001).build());
    }
    Long create(User pm, String title, String major, String stack, int capacity) {
        return projects.createProject(pm, new ProjectCreateRequest(title, title + " description", List.of(major),
                List.of(stack), LocalDate.now().plusDays(20), capacity));
    }
    void error(ErrorCode code, Runnable action) {
        assertEquals(code, assertThrows(ApiException.class, action::run).getErrorCode());
    }

    @Test void queryAndEachFilterApplyBeforePagination() {
        User pm=user("pm-search@test.invalid", "BE", "Spring");
        Long flutter=create(pm,"meal booking", "FE", "Flutter", 4);
        create(pm,"meal backend", "BE", "Spring", 4);
        create(pm,"chat client", "FE", "Flutter", 4);
        var result=projects.search("meal", "OPEN", "FE", "Flutter", 0, 1);
        assertEquals(1,result.totalElements());
        assertEquals(1,result.totalPages());
        assertEquals(flutter,result.items().get(0).id());
        assertEquals(0,projects.search("not matching", null, null, null, 0, 20).totalElements());
        error(ErrorCode.INVALID_FILTER, () -> projects.search(null,"INVALID",null,null,0,20));
        error(ErrorCode.INVALID_PAGINATION, () -> projects.search(null,null,null,null,-1,20));
    }

    @Test void recommendationsUseProfileMatchesAndExcludeFullProjectsAndMembership() {
        User pm=user("pm-rec@test.invalid", "BE", "Spring");
        User actor=user("rec@test.invalid", "FE", "Flutter");
        Long matching=create(pm,"matching", "FE", "Flutter", 4);
        create(pm,"unmatched", "AI", "Python", 4);
        Long full=create(pm,"full", "FE", "Flutter", 1);
        Long own=create(actor,"own", "FE", "Flutter", 4);
        var response=projects.recommended(actor,10);
        assertEquals("RULE_BASED",response.recommendationType());
        assertEquals(matching,response.items().get(0).id());
        assertTrue(response.items().stream().noneMatch(item -> item.id().equals(full)||item.id().equals(own)));
        error(ErrorCode.INVALID_SIZE, () -> projects.recommended(actor,0));
    }

    @Test void detailComputesRelationAndApplicationStatusServerSide() {
        User pm=user("pm-detail@test.invalid", "BE", "Spring"), actor=user("detail@test.invalid","FE","Flutter");
        Long id=create(pm,"detail","FE","Flutter",4);
        var before=projects.detail(actor,id);
        assertTrue(before.myRelation().canApply());
        assertFalse(before.myRelation().isMember());
        assertEquals(1,before.memberCount());
        Long application=applications.apply(actor,id,new ApplicationCreateRequest("I can build it","FE"));
        var pending=projects.detail(actor,id);
        assertFalse(pending.myRelation().canApply());
        assertEquals("PENDING",pending.myRelation().applicationStatus());
        applications.decide(pm,application,new ApplicationDecisionRequest("ACCEPTED"));
        var accepted=projects.detail(actor,id);
        assertTrue(accepted.myRelation().isMember());
        assertEquals("ACCEPTED",accepted.myRelation().applicationStatus());
        assertEquals(2,accepted.memberCount());
        assertTrue(accepted.members().stream().anyMatch(m -> m.role().equals("PM")));
    }

    @Test void projectInputAndCapacityHaveSpecificErrorCodes() {
        User pm=user("pm-input@test.invalid","BE","Spring");
        error(ErrorCode.INVALID_DEADLINE, () -> projects.createProject(pm,new ProjectCreateRequest("title","description",List.of("BE"),List.of("Spring"),LocalDate.now(),4)));
        error(ErrorCode.INVALID_CAPACITY, () -> projects.createProject(pm,new ProjectCreateRequest("title","description",List.of("BE"),List.of("Spring"),LocalDate.now().plusDays(20),13)));
        error(ErrorCode.INVALID_PROJECT_DATA, () -> projects.createProject(pm,new ProjectCreateRequest("","description",List.of("BE"),List.of("Spring"),LocalDate.now().plusDays(20),4)));
        Long id=create(pm,"capacity","BE","Spring",4);
        var updated=projects.update(pm,id,new ProjectUpdateRequest("new title",null,null,null,null,2,null));
        assertEquals("new title",updated.title());
        assertEquals(2,updated.capacity());
    }

    @Test void onlyPmCanEditAndCapacityCannotDropBelowMembership() {
        User pm=user("pm-edit@test.invalid","BE","Spring"), actor=user("edit@test.invalid","FE","Flutter");
        Long id=create(pm,"edit","FE","Flutter",4);
        Long application=applications.apply(actor,id,new ApplicationCreateRequest("reason","FE"));
        applications.decide(pm,application,new ApplicationDecisionRequest("ACCEPTED"));
        error(ErrorCode.NOT_PROJECT_PM, () -> projects.update(actor,id,new ProjectUpdateRequest("bad",null,null,null,null,null,null)));
        error(ErrorCode.CAPACITY_BELOW_MEMBER_COUNT, () -> projects.update(pm,id,new ProjectUpdateRequest(null,null,null,null,null,1,null)));
        assertEquals(2,members.countMembers(id));
    }

    @Test void stageTransitionsAreForwardAndCompletionClosesRecruitment() {
        User pm=user("pm-stage@test.invalid","BE","Spring"), actor=user("stage@test.invalid","FE","Flutter");
        Long id=create(pm,"stages","FE","Flutter",4);
        error(ErrorCode.INVALID_STAGE_TRANSITION, () -> projects.changeStage(pm,id,new ProjectStageRequest("COMPLETED")));
        projects.changeStage(pm,id,new ProjectStageRequest("IN_PROGRESS"));
        var completed=projects.changeStage(pm,id,new ProjectStageRequest("COMPLETED"));
        assertNotNull(completed.completedAt());
        assertTrue(completed.feedbackRequired());
        error(ErrorCode.INVALID_STAGE_TRANSITION, () -> projects.changeStage(pm,id,new ProjectStageRequest("IDEA")));
        error(ErrorCode.RECRUITMENT_CLOSED, () -> applications.apply(actor,id,new ApplicationCreateRequest("reason","FE")));
        assertEquals(1,projects.search(null,"CLOSED",null,null,0,20).totalElements());
    }

    @Test void canonicalDecisionIsNotRepeatableAndApplicantListsAreScoped() {
        User pm=user("pm-review@test.invalid","BE","Spring"), actor=user("review@test.invalid","FE","Flutter");
        Long id=create(pm,"review","FE","Flutter",4);
        var created=applications.create(actor,id,new ApplicationCreateRequest("reason","FE"));
        assertNotNull(created.createdAt());
        assertEquals(1,applications.projectApplications(pm,id,"PENDING",0,20).totalElements());
        assertEquals(1,applications.mine(actor,0,20).totalElements());
        assertEquals(0,applications.mine(pm,0,20).totalElements());
        error(ErrorCode.NOT_PROJECT_PM, () -> applications.projectApplications(actor,id,null,0,20));
        var decided=applications.decide(pm,created.id(),new ApplicationDecisionRequest("ACCEPTED"));
        assertEquals(actor.getUser_id(),decided.memberId());
        assertNotNull(decided.teamRoomId());
        error(ErrorCode.ALREADY_REVIEWED, () -> applications.decide(pm,created.id(),new ApplicationDecisionRequest("ACCEPTED")));
        assertEquals(2,members.countMembers(id));
    }

    @Test void cancellationRetainsHistoryButHidesWithdrawnApplications() {
        User pm=user("pm-cancel@test.invalid","BE","Spring"), actor=user("cancel@test.invalid","FE","Flutter");
        Long id=create(pm,"cancel","FE","Flutter",4);
        Long app=applications.apply(actor,id,new ApplicationCreateRequest("reason","FE"));
        applications.cancel(actor,app);
        assertEquals(0,applications.mine(actor,0,20).totalElements());
        assertEquals(0,applications.projectApplications(pm,id,null,0,20).totalElements());
        error(ErrorCode.APPLICATION_ALREADY_PROCESSED, () -> applications.cancel(actor,app));
        error(ErrorCode.ALREADY_REVIEWED, () -> applications.decide(pm,app,new ApplicationDecisionRequest("ACCEPTED")));
    }
}
