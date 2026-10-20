package com.F_A_T.F_A_T.domain.mypage.dto.request;

import jakarta.validation.constraints.Size;

import java.util.List;

public record MyPageUpdateRequest(
        String userMajor,
        List<String> interestStacks,
        @Size(max = 100) String bio,
        String clubName
) {}