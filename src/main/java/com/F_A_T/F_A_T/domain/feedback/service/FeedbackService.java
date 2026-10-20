package com.F_A_T.F_A_T.domain.feedback.service;

import com.F_A_T.F_A_T.domain.feedback.dto.request.FeedbackBulkCreateRequest;
import com.F_A_T.F_A_T.domain.feedback.dto.response.FeedbackResponse;
import com.F_A_T.F_A_T.domain.feedback.entity.Feedback;
import com.F_A_T.F_A_T.domain.feedback.repository.FeedbackRepository;
import com.F_A_T.F_A_T.domain.project.entity.Project;
import com.F_A_T.F_A_T.domain.project.entity.ProjectStatus;
import com.F_A_T.F_A_T.domain.project.repository.ProjectMemberRepository;
import com.F_A_T.F_A_T.domain.project.repository.ProjectRepository;
import com.F_A_T.F_A_T.domain.user.entity.User;
import com.F_A_T.F_A_T.domain.user.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository memberRepository;

    @Transactional
    public List<Long> createFeedbacks(User writer, Long projectId, FeedbackBulkCreateRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프로젝트입니다."));

        if (project.getProject_status() != ProjectStatus.COMPLETED) {
            throw new IllegalArgumentException("완료된 프로젝트만 피드백을 작성할 수 있습니다.");
        }

        if (!project.getPm().getUser_id().equals(writer.getUser_id())) {
            throw new AccessDeniedException("PM만 피드백을 작성할 수 있습니다.");
        }
        return request.feedbacks().stream()
                .map(item -> {
                    if (writer.getUser_id().equals(item.targetUserId()) || !memberRepository.existsMember(projectId, item.targetUserId())) {
                        throw new IllegalArgumentException("피드백 대상은 자신을 제외한 팀원이어야 합니다.");
                    }
                    User target = userRepository.findById(item.targetUserId())
                            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

                    Feedback feedback = Feedback.builder()
                            .project(project)
                            .writer(writer)
                            .target(target)
                            .rating(item.rating())
                            .feedback_content(item.content())
                            .build();

                    return feedbackRepository.save(feedback).getFeedback_id();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> getFeedbacks(Long projectId) {
        return feedbackRepository.findByProjectId(projectId).stream()
                .map(FeedbackResponse::from)
                .collect(Collectors.toList());
    }
}
