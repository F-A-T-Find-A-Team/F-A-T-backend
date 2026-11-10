package com.F_A_T.F_A_T.domain.mypage.dto.response;

import com.F_A_T.F_A_T.domain.user.entity.User;
import java.util.List;

/** GitHub integration is intentionally excluded. Private identifiers never enter PublicProfile. */
public record ProfileResponse(Long id, String email, String nickname, String name, String studentId,
        String gender, String bio, List<String> majors, List<String> techStacks, String avatarUrl,
        ProjectStats projectStats, FeedbackStats feedbackStats) {
    public record ProjectStats(long inProgress, long completed) {}
    public record FeedbackStats(double averageRating, long count) {}
    public record PublicProfile(Long id, String nickname, String bio, List<String> majors,
                                List<String> techStacks, String avatarUrl) {}
    public record UpdatedProfile(Long id, String nickname, String gender, String bio,
                                 List<String> majors, List<String> techStacks) {}

    public static ProfileResponse from(User u, ProjectStats projects, FeedbackStats feedback) {
        return new ProfileResponse(u.getUser_id(), u.getUser_email(), u.getNickname(), u.getRealName(),
                u.getStudentId(), u.getUser_gender(), u.getUser_bio(), List.copyOf(u.getMajors()),
                List.copyOf(u.getInterest_stacks()), u.getAvatar_url(), projects, feedback);
    }
    public static PublicProfile publicFrom(User u) {
        return new PublicProfile(u.getUser_id(), u.getNickname(), u.getUser_bio(),
                List.copyOf(u.getMajors()), List.copyOf(u.getInterest_stacks()), u.getAvatar_url());
    }
    public static UpdatedProfile updatedFrom(User u) {
        return new UpdatedProfile(u.getUser_id(), u.getNickname(), u.getUser_gender(), u.getUser_bio(),
                List.copyOf(u.getMajors()), List.copyOf(u.getInterest_stacks()));
    }
}
