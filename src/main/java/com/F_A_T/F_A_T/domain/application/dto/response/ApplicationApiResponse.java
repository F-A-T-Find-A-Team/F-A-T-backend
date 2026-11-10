package com.F_A_T.F_A_T.domain.application.dto.response;

import com.F_A_T.F_A_T.domain.application.entity.ApplicationStatus;
import java.time.Instant;
import java.util.List;

public final class ApplicationApiResponse {
    private ApplicationApiResponse() {}
    public record Created(Long id, Long projectId, Long applicantId, String position, String reason,
                          ApplicationStatus status, Instant createdAt) {}
    public record Applicant(Long id, String nickname, List<String> majors) {}
    public record ApplicantItem(Long id, Applicant applicant, String position, String reason, ApplicationStatus status) {}
    public record ProjectInfo(Long id, String title) {}
    public record MineItem(Long id, ProjectInfo project, String position, ApplicationStatus status, Instant createdAt) {}
    public record Page<T>(List<T> items, int page, int size, long totalElements) {}
    public record Decision(Long id, ApplicationStatus status, Long projectId, Long memberId, Long teamRoomId) {}
}
