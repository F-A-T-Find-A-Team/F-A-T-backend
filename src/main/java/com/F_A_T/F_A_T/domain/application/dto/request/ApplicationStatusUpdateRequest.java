package com.F_A_T.F_A_T.domain.application.dto.request;

import com.F_A_T.F_A_T.domain.application.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record ApplicationStatusUpdateRequest(
        @NotNull ApplicationStatus status
) {}