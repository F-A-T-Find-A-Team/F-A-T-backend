package com.F_A_T.F_A_T.domain.project.service;

import com.F_A_T.F_A_T.domain.chat.entity.ChatParticipant;
import com.F_A_T.F_A_T.domain.chat.entity.ChatRoom;
import com.F_A_T.F_A_T.domain.chat.repository.ChatParticipantRepository;
import com.F_A_T.F_A_T.domain.chat.repository.ChatRoomRepository;
import com.F_A_T.F_A_T.domain.project.dto.request.ProjectCreateRequest;
import com.F_A_T.F_A_T.domain.project.dto.request.ProjectStatusUpdateRequest;
import com.F_A_T.F_A_T.domain.project.dto.response.ProjectListResponse;
import com.F_A_T.F_A_T.domain.project.dto.response.ProjectMemberResponse;
import com.F_A_T.F_A_T.domain.project.dto.response.ProjectResponse;
import com.F_A_T.F_A_T.domain.project.entity.Project;
import com.F_A_T.F_A_T.domain.project.entity.ProjectMember;
import com.F_A_T.F_A_T.domain.project.repository.ProjectMemberRepository;
import com.F_A_T.F_A_T.domain.project.repository.ProjectRepository;
import com.F_A_T.F_A_T.domain.user.entity.User;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final ChatRoomRepository roomRepository;
    private final ChatParticipantRepository participantRepository;

    @Transactional
    public Long createProject(User pm, ProjectCreateRequest request) {
        Project project = Project.builder()
                .pm(pm)
                .project_title(request.projectTitle())
                .project_description(request.projectDescription())
                .required_majors(request.requiredMajors())
                .required_stacks(request.requiredStacks())
                .project_deadline(request.projectDeadline())
                .recruit_limit(request.recruitLimit())
                .build();

        projectRepository.save(project);
        memberRepository.save(new ProjectMember(project, pm));
        var room = roomRepository.save(ChatRoom.builder().project(project).build());
        participantRepository.save(new ChatParticipant(room, pm));
        return project.getProject_id();
    }

    @Transactional
    public ProjectResponse getProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프로젝트입니다."));

        project.increaseViewCount();

        int currentMemberCount = memberRepository.countMembers(project.getProject_id());

        return ProjectResponse.from(project, currentMemberCount);
    }

    @Transactional
    public void updateStatus(User actor, Long projectId, ProjectStatusUpdateRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프로젝트입니다."));

        if (!project.getPm().getUser_id().equals(actor.getUser_id())) {
            throw new AccessDeniedException("프로젝트 관리자만 상태를 변경할 수 있습니다.");
        }
        project.changeStatus(request.status());
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> getMembers(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프로젝트입니다."));

        return memberRepository.findMembers(projectId).stream()
                .map(user -> ProjectMemberResponse.from(user, project.getPm().getUser_id()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectListResponse> getProjectList() {
        return projectRepository.findAll().stream()
                .map(project -> {
                    int currentMemberCount = memberRepository.countMembers(project.getProject_id());
                    return ProjectListResponse.from(project, currentMemberCount);
                })
                .collect(Collectors.toList());
    }
}
