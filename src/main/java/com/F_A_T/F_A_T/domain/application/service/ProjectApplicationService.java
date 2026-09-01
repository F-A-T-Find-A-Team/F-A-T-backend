package com.F_A_T.F_A_T.domain.application.service;

import com.F_A_T.F_A_T.domain.application.dto.request.ApplicationCreateRequest;
import com.F_A_T.F_A_T.domain.application.dto.request.ApplicationStatusUpdateRequest;
import com.F_A_T.F_A_T.domain.application.entity.ProjectApplication;
import com.F_A_T.F_A_T.domain.application.repository.ProjectApplicationRepository;
import com.F_A_T.F_A_T.domain.project.entity.Project;
import com.F_A_T.F_A_T.domain.project.repository.ProjectRepository;
import com.F_A_T.F_A_T.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectApplicationService {

    private final ProjectApplicationRepository applicationRepository;
    private final ProjectRepository projectRepository;

    @Transactional
    public Long apply(User applicant, Long projectId, ApplicationCreateRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프로젝트입니다."));

        if (applicationRepository.existsByProjectAndApplicant(project, applicant)) {
            throw new IllegalArgumentException("이미 지원한 프로젝트입니다.");
        }

        ProjectApplication application = ProjectApplication.builder()
                .project(project)
                .applicant(applicant)
                .application_message(request.message())
                .build();

        return applicationRepository.save(application).getApplication_id();
    }

    @Transactional
    public void updateStatus(Long applicationId, ApplicationStatusUpdateRequest request) {
        ProjectApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지원 내역입니다."));

        application.changeStatus(request.status());
    }
}
