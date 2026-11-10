package com.F_A_T.F_A_T.domain.project.dto.response;

import com.F_A_T.F_A_T.domain.project.entity.ProjectStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class ProjectApiResponse {
    private ProjectApiResponse() {}
    public record Pm(Long id, String nickname) {}
    public record Member(Long userId, String nickname, String role, List<String> majors, String avatarUrl) {}
    public record Card(Long id, String title, String summary, Pm pm, ProjectStatus stage,
                       String recruitmentStatus, List<String> requiredMajors, List<String> techStacks,
                       LocalDate deadline, int memberCount, Integer capacity, Integer viewCount) {}
    public record Page<T>(List<T> items, int page, int size, long totalElements, int totalPages) {}
    public record Created(Long id, String title, ProjectStatus stage, String recruitmentStatus,
                          Long pmId, int memberCount, Integer capacity, Instant createdAt) {}
    public record MyRelation(boolean isPm, boolean isMember, String applicationStatus,
                             boolean canApply, boolean canChangeStage) {}
    public record Detail(Long id, String title, String description, Pm pm, ProjectStatus stage,
                         String recruitmentStatus, List<String> requiredMajors, List<String> techStacks,
                         LocalDate deadline, Integer capacity, int memberCount, Integer viewCount,
                         List<Member> members, MyRelation myRelation) {}
    public record Updated(Long id, String title, String description, Integer capacity, LocalDate deadline) {}
    public record StageChanged(Long id, ProjectStatus stage, Instant completedAt, boolean feedbackRequired) {}
    public record Members(List<Member> items, Integer capacity) {}
    public record Recommended(List<Card> items, String recommendationType) {}
}
