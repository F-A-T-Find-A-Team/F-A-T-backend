package com.F_A_T.F_A_T.domain.chat.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChatMessageCreateRequest(
        @NotBlank String content
) {}