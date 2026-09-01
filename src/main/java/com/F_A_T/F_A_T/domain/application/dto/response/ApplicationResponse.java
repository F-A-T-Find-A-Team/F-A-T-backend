package com.F_A_T.F_A_T.domain.application.dto.response;

import com.F_A_T.F_A_T.domain.application.entity.ApplicationStatus;
import com.F_A_T.F_A_T.domain.application.entity.ProjectApplication;

public record ApplicationResponse(
        Long applicationId,
        Long projectId,
        Long applicantId,
        String message,
        ApplicationStatus status
) {
    public static ApplicationResponse from(ProjectApplication application) {
        return new ApplicationResponse(
                application.getApplication_id(),
                application.getProject().getProject_id(),
                application.getApplicant().getUser_id(),
                application.getApplication_message(),
                application.getApplication_status()
        );
    }
}