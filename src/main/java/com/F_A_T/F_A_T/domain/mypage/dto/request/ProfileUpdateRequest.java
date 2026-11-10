package com.F_A_T.F_A_T.domain.mypage.dto.request;

import jakarta.validation.constraints.Size;
import java.util.List;

public record ProfileUpdateRequest(
        @Size(min = 1, max = 40) String nickname,
        @Size(min = 1, max = 32) String gender,
        @Size(max = 100) String bio,
        @Size(min = 1, max = 8) List<String> majors,
        @Size(max = 40) List<String> techStacks
) {}
