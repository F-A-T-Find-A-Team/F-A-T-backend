package com.F_A_T.F_A_T.domain.feedback.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record FeedbackBulkCreateRequest(
        @NotEmpty @Valid List<FeedbackItem> feedbacks
) {
    public record FeedbackItem(
            @NotNull Long targetUserId,
            @NotNull @Min(1) @Max(5) Integer rating,
            String content
    ) {}
}