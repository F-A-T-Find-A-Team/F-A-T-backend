package com.F_A_T.F_A_T.domain.mypage.dto.response;

import com.F_A_T.F_A_T.domain.user.entity.User;
import java.util.List;

public record MyPageResponse(
        Long userId,
        String userEmail,
        String userName,
        String userGender,
        String userMajor,
        List<String> interestStacks,
        Integer userStudentNumber,
        String bio,
        String clubName,
        String avatarUrl
) {
    public static MyPageResponse from(User user) {
        return new MyPageResponse(
                user.getUser_id(),
                user.getUser_email(),
                user.getUser_name(),
                user.getUser_gender(),
                user.getUser_major(),
                List.copyOf(user.getInterest_stacks()),
                user.getUser_student_number(),
                user.getUser_bio(),
                user.getClub_name(),
                user.getAvatar_url()
        );
    }
}
