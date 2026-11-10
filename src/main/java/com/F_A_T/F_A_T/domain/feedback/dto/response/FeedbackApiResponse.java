package com.F_A_T.F_A_T.domain.feedback.dto.response;

import java.time.Instant;
import java.util.List;

public final class FeedbackApiResponse {
    private FeedbackApiResponse() {}
    public record Submitted(Long projectId, int submittedCount, Instant submittedAt) {}
    public record Reviewer(Long id, String nickname) {}
    public record ProjectInfo(Long id, String title) {}
    public record ReviewItem(Long id, Reviewer reviewer, ProjectInfo project, Integer rating, String comment, Instant createdAt) {}
    public record Received(double averageRating, long totalReviews, List<ReviewItem> items, int page, int size, long totalElements) {}
    public record Status(Long projectId, boolean isCompleted, List<Long> reviewedUserIds, List<Long> pendingUserIds) {}
}
