package com.F_A_T.F_A_T.domain.project.dto.request;

import com.F_A_T.F_A_T.domain.project.entity.ProjectStatus;
import jakarta.validation.constraints.NotNull;

public record ProjectStatusUpdateRequest(
        @NotNull com.F_A_T.F_A_T.domain.project.dto.ProjectStatus status
) {}