package com.F_A_T.F_A_T.domain.feedback.dto.response;

import com.F_A_T.F_A_T.domain.feedback.entity.Feedback;

public record FeedbackResponse(
        Long feedbackId,
        Long writerId,
        Long targetId,
        String content
) {
    public static FeedbackResponse from(Feedback feedback) {
        return new FeedbackResponse(
                feedback.getFeedback_id(),
                feedback.getWriter().getUser_id(),
                feedback.getTarget().getUser_id(),
                feedback.getFeedback_content()
        );
    }
}