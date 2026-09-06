package com.F_A_T.F_A_T.domain.mypage.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GithubConnectRequest(
        @NotBlank String githubUsername
) {}