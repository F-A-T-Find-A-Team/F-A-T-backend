package com.F_A_T.F_A_T.domain.project.dto.response;

import com.F_A_T.F_A_T.domain.project.entity.Project;
import com.F_A_T.F_A_T.domain.project.entity.ProjectStatus;

import java.time.LocalDate;
import java.util.List;

public record ProjectListResponse(
        Long projectId,
        String projectTitle,
        List<String> requiredMajors,
        List<String> requiredStacks,
        LocalDate projectDeadline,
        ProjectStatus projectStatus,
        Integer recruitLimit,
        Integer currentMemberCount
) {
    public static ProjectListResponse from(Project project, int currentMemberCount) {
        return new ProjectListResponse(
                project.getProject_id(),
                project.getProject_title(),
                List.copyOf(project.getRequired_majors()),
                List.copyOf(project.getRequired_stacks()),
                project.getProject_deadline(),
                project.getProject_status(),
                project.getRecruit_limit(),
                currentMemberCount
        );
    }
}