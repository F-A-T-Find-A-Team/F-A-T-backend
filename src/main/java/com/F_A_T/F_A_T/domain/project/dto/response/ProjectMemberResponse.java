package com.F_A_T.F_A_T.domain.project.dto.response;

import com.F_A_T.F_A_T.domain.user.entity.User;

public record ProjectMemberResponse(Long userId, String userName, String userMajor, String avatarUrl, String role) {
    public static ProjectMemberResponse from(User user, Long pmId) {
        return new ProjectMemberResponse(user.getUser_id(), user.getUser_name(), user.getUser_major(),
                user.getAvatar_url(), user.getUser_id().equals(pmId) ? "PM" : "MEMBER");
    }
}
