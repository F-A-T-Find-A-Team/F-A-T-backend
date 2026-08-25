package com.F_A_T.F_A_T.domain.project.dto.response;

import com.F_A_T.F_A_T.domain.project.entity.Project;
import com.F_A_T.F_A_T.domain.project.entity.ProjectStatus;

import java.time.LocalDate;
import java.util.List;

public record ProjectResponse(
        Long projectId,
        Long pmId,
        String projectTitle,
        String projectDescription,
        List<String> requiredMajors,
        List<String> requiredStacks,
        LocalDate projectDeadline,
        ProjectStatus projectStatus
) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getProject_id(),
                project.getPm().getUser_id(),
                project.getProject_title(),
                project.getProject_description(),
                project.getRequired_majors(),
                project.getRequired_stacks(),
                project.getProject_deadline(),
                project.getProject_status()
        );
    }
}