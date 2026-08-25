package com.F_A_T.F_A_T.domain.project.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record ProjectCreateRequest(
        @NotBlank String projectTitle,
        @NotBlank String projectDescription,
        List<String> requiredMajors,
        List<String> requiredStacks,
        @NotNull LocalDate projectDeadline
) {}