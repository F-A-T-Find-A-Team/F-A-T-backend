package com.F_A_T.F_A_T.domain.mypage.dto.request;

import java.util.List;

public record MyPageUpdateRequest(
        String userMajor,
        List<String> interestStacks
) {}