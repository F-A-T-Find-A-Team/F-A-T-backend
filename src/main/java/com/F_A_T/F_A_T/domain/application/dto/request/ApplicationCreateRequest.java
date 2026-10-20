package com.F_A_T.F_A_T.domain.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ApplicationCreateRequest(
        @NotBlank String message,
        @NotBlank String position
) {}
