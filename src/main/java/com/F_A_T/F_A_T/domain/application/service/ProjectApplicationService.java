package com.F_A_T.F_A_T.domain.application.service;

import com.F_A_T.F_A_T.domain.application.dto.request.ApplicationCreateRequest;
import com.F_A_T.F_A_T.domain.application.dto.request.ApplicationStatusUpdateRequest;
import com.F_A_T.F_A_T.domain.application.entity.ApplicationStatus;
import com.F_A_T.F_A_T.domain.application.entity.ProjectApplication;
import com.F_A_T.F_A_T.domain.application.repository.ProjectApplicationRepository;
import com.F_A_T.F_A_T.domain.chat.entity.ChatParticipant;
import com.F_A_T.F_A_T.domain.chat.entity.ChatRoom;
import com.F_A_T.F_A_T.domain.chat.repository.ChatParticipantRepository;
import com.F_A_T.F_A_T.domain.chat.repository.ChatRoomRepository;
import com.F_A_T.F_A_T.domain.project.entity.Project;
import com.F_A_T.F_A_T.domain.project.entity.ProjectMember;
import com.F_A_T.F_A_T.domain.project.repository.ProjectMemberRepository;
import com.F_A_T.F_A_T.domain.project.repository.ProjectRepository;
import com.F_A_T.F_A_T.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
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

    @Transactional
    public Long apply(User applicant, Long projectId, ApplicationCreateRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프로젝트입니다."));

        if (!project.isRecruiting_open()) {
            throw new IllegalArgumentException("모집 중인 프로젝트가 아닙니다.");
        }
        if (memberRepository.existsMember(projectId, applicant.getUser_id())) {
            throw new IllegalArgumentException("이미 프로젝트 팀원입니다.");
        }
        if (applicationRepository.existsByProjectAndApplicant(project, applicant)) {
            throw new IllegalArgumentException("이미 지원한 프로젝트입니다.");
        }

        ProjectApplication application = ProjectApplication.builder()
                .project(project)
                .applicant(applicant)
                .application_message(request.message())
                .application_position(request.position())
                .build();

        return applicationRepository.save(application).getApplication_id();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void updateStatus(User actor, Long applicationId, ApplicationStatusUpdateRequest request) {
        ProjectApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지원 내역입니다."));

        Project project = projectRepository.findForUpdate(application.getProject().getProject_id())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프로젝트입니다."));
        // Refresh after acquiring the aggregate lock: a concurrent decision may have committed.
        entityManager.flush();
        entityManager.refresh(application);
        if (!project.getPm().getUser_id().equals(actor.getUser_id())) {
            throw new AccessDeniedException("프로젝트 관리자만 지원 상태를 변경할 수 있습니다.");
        }
        if (request.status() != ApplicationStatus.ACCEPTED && request.status() != ApplicationStatus.REJECTED) {
            throw new IllegalArgumentException("PM은 수락 또는 거절만 할 수 있습니다.");
        }
        if (application.getApplication_status() == ApplicationStatus.CANCELLED) {
            throw new IllegalArgumentException("취소된 지원은 변경할 수 없습니다.");
        }
        var accepted = ApplicationStatus.ACCEPTED;
        if (application.getApplication_status() == accepted && request.status() != accepted) {
            throw new IllegalArgumentException("수락 취소는 별도의 팀원 탈퇴 정책이 필요합니다.");
        }
        if (request.status() == accepted && !memberRepository.existsMember(project.getProject_id(), application.getApplicant().getUser_id())) {
            if (memberRepository.countMembers(project.getProject_id()) >= project.getRecruit_limit()) {
                throw new IllegalArgumentException("모집 정원이 찼습니다.");
            }
            memberRepository.save(new ProjectMember(project, application.getApplicant()));
            var room = roomRepository.findByProjectId(project.getProject_id()).orElseGet(() -> {
                var created = roomRepository.save(ChatRoom.builder().project(project).build());
                for (User member : memberRepository.findMembers(project.getProject_id())) {
                    participantRepository.save(new ChatParticipant(created, member));
                }
                return created;
            });
            if (!participantRepository.existsParticipant(room.getChat_room_id(), application.getApplicant().getUser_id())) {
                participantRepository.save(new ChatParticipant(room, application.getApplicant()));
            }
        }
        application.changeStatus(request.status());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void cancel(User actor, Long applicationId) {
        ProjectApplication application = applicationRepository.findById(applicationId).orElseThrow();
        projectRepository.findForUpdate(application.getProject().getProject_id()).orElseThrow();
        entityManager.flush();
        entityManager.refresh(application);
        if (!application.getApplicant().getUser_id().equals(actor.getUser_id())) {
            throw new AccessDeniedException("본인의 지원만 취소할 수 있습니다.");
        }
        if (application.getApplication_status() != ApplicationStatus.PENDING) {
            throw new IllegalArgumentException("대기 중인 지원만 취소할 수 있습니다.");
        }
        application.changeStatus(ApplicationStatus.CANCELLED);
    }
}
