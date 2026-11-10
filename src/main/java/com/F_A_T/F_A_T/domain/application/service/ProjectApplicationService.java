package com.F_A_T.F_A_T.domain.application.service;

import com.F_A_T.F_A_T.domain.application.dto.request.*;
import com.F_A_T.F_A_T.domain.application.dto.response.ApplicationApiResponse;
import com.F_A_T.F_A_T.domain.application.entity.*;
import com.F_A_T.F_A_T.domain.application.repository.ProjectApplicationRepository;
import com.F_A_T.F_A_T.domain.chat.entity.*;
import com.F_A_T.F_A_T.domain.chat.repository.*;
import com.F_A_T.F_A_T.domain.project.entity.*;
import com.F_A_T.F_A_T.domain.project.repository.*;
import com.F_A_T.F_A_T.domain.project.service.ProjectService;
import com.F_A_T.F_A_T.domain.user.entity.User;
import com.F_A_T.F_A_T.global.exception.ApiException;
import com.F_A_T.F_A_T.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectApplicationService {
    private final EntityManager entityManager;
    private final ProjectApplicationRepository applicationRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final ChatRoomRepository roomRepository;
    private final ChatParticipantRepository participantRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Long apply(User applicant, Long projectId, ApplicationCreateRequest r) {
        if (r == null || r.reason() == null || r.reason().isBlank() || r.reason().length() > 255 ||
                r.position() == null || r.position().isBlank() || r.position().length() > 255)
            throw new ApiException(ErrorCode.INVALID_APPLICATION_DATA);
        Project p = lockProject(projectId);
        if (p.getPm().getUser_id().equals(applicant.getUser_id())) throw new ApiException(ErrorCode.CANNOT_APPLY_OWN_PROJECT);
        if (memberRepository.existsMember(projectId, applicant.getUser_id()) || applicationRepository.existsByProjectAndApplicant(p, applicant))
            throw new ApiException(ErrorCode.ALREADY_APPLIED);
        if (!p.isRecruiting_open() || p.getProject_status() == ProjectStatus.COMPLETED || p.getProject_deadline().isBefore(LocalDate.now()))
            throw new ApiException(ErrorCode.RECRUITMENT_CLOSED);
        if (memberRepository.countMembers(projectId) >= p.getRecruit_limit()) throw new ApiException(ErrorCode.PROJECT_FULL);
        var application = ProjectApplication.builder().project(p).applicant(applicant)
                .application_message(r.reason()).application_position(r.position()).build();
        return applicationRepository.save(application).getApplication_id();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ApplicationApiResponse.Created create(User actor, Long projectId, ApplicationCreateRequest r) {
        Long id = apply(actor, projectId, r);
        var a = find(id);
        return new ApplicationApiResponse.Created(id, projectId, actor.getUser_id(), a.getApplication_position(),
                a.getApplication_message(), a.getApplication_status(), a.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public ApplicationApiResponse.Page<ApplicationApiResponse.ApplicantItem> projectApplications(User actor, Long projectId,
            String status, int page, int size) {
        ProjectService.validatePagination(page, size);
        var p = projectRepository.findById(projectId).orElseThrow(() -> new ApiException(ErrorCode.PROJECT_NOT_FOUND));
        requirePm(p, actor);
        ApplicationStatus filter = null;
        if (status != null) {
            try { filter = ApplicationStatus.valueOf(status); }
            catch (RuntimeException invalid) { throw new ApiException(ErrorCode.INVALID_APPLICATION_STATUS); }
            if (filter == ApplicationStatus.CANCELLED) throw new ApiException(ErrorCode.INVALID_APPLICATION_STATUS);
        }
        var result = applicationRepository.findProjectApplications(projectId, filter,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "application_id")));
        var items = result.getContent().stream().map(a -> new ApplicationApiResponse.ApplicantItem(a.getApplication_id(),
                new ApplicationApiResponse.Applicant(a.getApplicant().getUser_id(), a.getApplicant().getNickname(),
                        List.copyOf(a.getApplicant().getMajors())), a.getApplication_position(), a.getApplication_message(), a.getApplication_status())).toList();
        return new ApplicationApiResponse.Page<>(items, page, size, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ApplicationApiResponse.Page<ApplicationApiResponse.MineItem> mine(User actor, int page, int size) {
        ProjectService.validatePagination(page, size);
        var result = applicationRepository.findMyApplications(actor.getUser_id(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "application_id")));
        var items = result.getContent().stream().map(a -> new ApplicationApiResponse.MineItem(a.getApplication_id(),
                new ApplicationApiResponse.ProjectInfo(a.getProject().getProject_id(), a.getProject().getProject_title()),
                a.getApplication_position(), a.getApplication_status(), a.getCreatedAt())).toList();
        return new ApplicationApiResponse.Page<>(items, page, size, result.getTotalElements());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ApplicationApiResponse.Decision decide(User actor, Long id, ApplicationDecisionRequest r) {
        ApplicationStatus next;
        try { next = ApplicationStatus.valueOf(r.status()); }
        catch (RuntimeException invalid) { throw new ApiException(ErrorCode.INVALID_APPLICATION_STATUS); }
        if (next != ApplicationStatus.ACCEPTED && next != ApplicationStatus.REJECTED) throw new ApiException(ErrorCode.INVALID_APPLICATION_STATUS);
        var a = lockedApplication(id);
        requirePm(a.getProject(), actor);
        if (a.getApplication_status() != ApplicationStatus.PENDING) throw new ApiException(ErrorCode.ALREADY_REVIEWED);
        review(a, next);
        Long room = next == ApplicationStatus.ACCEPTED ? roomRepository.findByProjectId(a.getProject().getProject_id()).orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_SERVER_ERROR)).getChat_room_id() : null;
        return new ApplicationApiResponse.Decision(id, next, a.getProject().getProject_id(),
                next == ApplicationStatus.ACCEPTED ? a.getApplicant().getUser_id() : null, room);
    }

    // Kept for internal callers that retry an already accepted decision idempotently.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void updateStatus(User actor, Long id, ApplicationStatusUpdateRequest r) {
        if (r == null || (r.status() != ApplicationStatus.ACCEPTED && r.status() != ApplicationStatus.REJECTED))
            throw new ApiException(ErrorCode.INVALID_APPLICATION_STATUS);
        var a = lockedApplication(id); requirePm(a.getProject(), actor);
        if (a.getApplication_status() == ApplicationStatus.ACCEPTED && r.status() == ApplicationStatus.ACCEPTED) return;
        if (a.getApplication_status() != ApplicationStatus.PENDING) throw new ApiException(ErrorCode.ALREADY_REVIEWED);
        review(a, r.status());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void cancel(User actor, Long id) {
        var a = lockedApplication(id);
        if (!a.getApplicant().getUser_id().equals(actor.getUser_id())) throw new ApiException(ErrorCode.NOT_APPLICATION_OWNER);
        if (a.getApplication_status() != ApplicationStatus.PENDING) throw new ApiException(ErrorCode.APPLICATION_ALREADY_PROCESSED);
        a.changeStatus(ApplicationStatus.CANCELLED);
    }

    private void review(ProjectApplication a, ApplicationStatus next) {
        var p = a.getProject();
        if (next == ApplicationStatus.ACCEPTED) {
            if (!p.isRecruiting_open() || p.getProject_status() == ProjectStatus.COMPLETED || p.getProject_deadline().isBefore(LocalDate.now()))
                throw new ApiException(ErrorCode.RECRUITMENT_CLOSED);
            if (memberRepository.countMembers(p.getProject_id()) >= p.getRecruit_limit()) throw new ApiException(ErrorCode.PROJECT_FULL);
            if (memberRepository.existsMember(p.getProject_id(), a.getApplicant().getUser_id())) throw new ApiException(ErrorCode.ALREADY_REVIEWED);
            memberRepository.save(new ProjectMember(p, a.getApplicant()));
            var room = roomRepository.findByProjectId(p.getProject_id()).orElseGet(() -> {
                var created = roomRepository.save(ChatRoom.builder().project(p).build());
                for (User member : memberRepository.findMembers(p.getProject_id())) participantRepository.save(new ChatParticipant(created, member));
                return created;
            });
            if (!participantRepository.existsParticipant(room.getChat_room_id(), a.getApplicant().getUser_id()))
                participantRepository.save(new ChatParticipant(room, a.getApplicant()));
        }
        a.changeStatus(next);
    }
    private Project lockProject(Long id) {
        if (id == null || id < 1) throw new ApiException(ErrorCode.INVALID_PROJECT_ID);
        return projectRepository.findForUpdate(id).orElseThrow(() -> new ApiException(ErrorCode.PROJECT_NOT_FOUND));
    }
    private ProjectApplication find(Long id) {
        if (id == null || id < 1) throw new ApiException(ErrorCode.APPLICATION_NOT_FOUND);
        return applicationRepository.findById(id).orElseThrow(() -> new ApiException(ErrorCode.APPLICATION_NOT_FOUND));
    }
    private ProjectApplication lockedApplication(Long id) {
        var a = find(id);
        lockProject(a.getProject().getProject_id());
        entityManager.flush(); entityManager.refresh(a);
        return a;
    }
    private void requirePm(Project p, User actor) {
        if (!p.getPm().getUser_id().equals(actor.getUser_id())) throw new ApiException(ErrorCode.NOT_PROJECT_PM);
    }
}
