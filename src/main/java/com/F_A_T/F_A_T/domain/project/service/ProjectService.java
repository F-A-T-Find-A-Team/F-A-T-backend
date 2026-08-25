package com.F_A_T.F_A_T.domain.project.service;

import com.F_A_T.F_A_T.domain.project.dto.request.ProjectCreateRequest;
import com.F_A_T.F_A_T.domain.project.dto.request.ProjectStatusUpdateRequest;
import com.F_A_T.F_A_T.domain.project.dto.response.ProjectResponse;
import com.F_A_T.F_A_T.domain.project.entity.Project;
import com.F_A_T.F_A_T.domain.project.repository.ProjectRepository;
import com.F_A_T.F_A_T.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    @Transactional
    public Long createProject(User pm, ProjectCreateRequest request) {
        Project project = Project.builder()
                .pm(pm)
                .project_title(request.projectTitle())
                .project_description(request.projectDescription())
                .required_majors(request.requiredMajors())
                .required_stacks(request.requiredStacks())
                .project_deadline(request.projectDeadline())
                .build();

        return projectRepository.save(project).getProject_id();
    }

    public ProjectResponse getProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프로젝트입니다."));

        return ProjectResponse.from(project);
    }

    @Transactional
    public void updateStatus(Long projectId, ProjectStatusUpdateRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프로젝트입니다."));

        project.changeStatus(request.status());
    }
}
