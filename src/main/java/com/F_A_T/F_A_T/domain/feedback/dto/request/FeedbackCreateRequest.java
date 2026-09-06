package com.F_A_T.F_A_T.domain.feedback.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FeedbackCreateRequest(
        @NotNull Long targetUserId,
        @NotBlank String content
) {}