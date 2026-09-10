package com.F_A_T.F_A_T.domain.project.dto.response;

import com.F_A_T.F_A_T.domain.user.entity.User;

public record ProjectMemberResponse(
        Long userId,
        String userName
) {
    public static ProjectMemberResponse from(User user) {
        return new ProjectMemberResponse(
                user.getUser_id(),
                user.getUser_name()
        );
    }
}