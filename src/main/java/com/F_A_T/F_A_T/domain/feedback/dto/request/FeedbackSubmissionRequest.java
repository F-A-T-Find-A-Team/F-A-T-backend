package com.F_A_T.F_A_T.domain.feedback.dto.request;

import java.util.List;

public record FeedbackSubmissionRequest(List<Review> reviews) {
    public record Review(Long revieweeId, Integer rating, String comment) {}
}
