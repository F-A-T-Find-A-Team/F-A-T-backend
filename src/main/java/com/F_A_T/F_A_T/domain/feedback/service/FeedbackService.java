package com.F_A_T.F_A_T.domain.feedback.service;

import com.F_A_T.F_A_T.domain.feedback.dto.request.*;
import com.F_A_T.F_A_T.domain.feedback.dto.response.*;
import com.F_A_T.F_A_T.domain.feedback.entity.Feedback;
import com.F_A_T.F_A_T.domain.feedback.repository.FeedbackRepository;
import com.F_A_T.F_A_T.domain.project.entity.*;
import com.F_A_T.F_A_T.domain.project.repository.*;
import com.F_A_T.F_A_T.domain.user.entity.User;
import com.F_A_T.F_A_T.domain.user.repository.UserRepository;
import com.F_A_T.F_A_T.global.exception.*;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository memberRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FeedbackApiResponse.Submitted submit(User writer, Long projectId, FeedbackSubmissionRequest request) {
        if (request == null || request.reviews() == null) throw new ApiException(ErrorCode.INVALID_FEEDBACK_DATA);
        List<Long> ids = saveReviews(writer, projectId, request.reviews());
        return new FeedbackApiResponse.Submitted(projectId, ids.size(), Instant.now());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<Long> createFeedbacks(User writer, Long projectId, FeedbackBulkCreateRequest request) {
        if (request == null || request.feedbacks() == null) throw new ApiException(ErrorCode.INVALID_FEEDBACK_DATA);
        return saveReviews(writer, projectId, request.feedbacks().stream()
                .map(item -> new FeedbackSubmissionRequest.Review(item.targetUserId(), item.rating(), item.content())).toList());
    }

    private List<Long> saveReviews(User writer, Long projectId, List<FeedbackSubmissionRequest.Review> reviews) {
        Project project = projectRepository.findForUpdate(projectId).orElseThrow(() -> new ApiException(ErrorCode.PROJECT_NOT_FOUND));
        requirePm(writer, project, "평가 권한이 없습니다.");
        if (project.getProject_status() != ProjectStatus.COMPLETED) throw new ApiException(ErrorCode.PROJECT_NOT_COMPLETED);
        if (reviews.isEmpty() || reviews.size() > 100) throw new ApiException(ErrorCode.INVALID_FEEDBACK_DATA);
        Set<Long> seen = new HashSet<>();
        List<User> targets = new ArrayList<>();
        // Validate the whole batch before writing a single review.
        for (var item : reviews) {
            if (item == null || item.revieweeId() == null || item.rating() == null || item.rating() < 1 || item.rating() > 5
                    || !seen.add(item.revieweeId()) || writer.getUser_id().equals(item.revieweeId())
                    || (item.comment() != null && item.comment().length() > 10000)
                    || !memberRepository.existsMember(projectId, item.revieweeId())) throw new ApiException(ErrorCode.INVALID_FEEDBACK_DATA);
            if (feedbackRepository.existsReview(projectId, writer.getUser_id(), item.revieweeId())) throw new ApiException(ErrorCode.ALREADY_REVIEWED, "이미 평가한 팀원이 있습니다.");
            targets.add(userRepository.findById(item.revieweeId()).orElseThrow(() -> new ApiException(ErrorCode.INVALID_FEEDBACK_DATA)));
        }
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < reviews.size(); i++) {
            var item = reviews.get(i);
            Feedback feedback = Feedback.builder().project(project).writer(writer).target(targets.get(i))
                    .rating(item.rating()).feedback_content(item.comment()).build();
            ids.add(feedbackRepository.save(feedback).getFeedback_id());
        }
        feedbackRepository.flush();
        return ids;
    }

    @Transactional(readOnly = true)
    public FeedbackApiResponse.Received received(User actor, int page, int size) {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) throw new ApiException(ErrorCode.INVALID_PAGINATION);
        if (!userRepository.existsById(actor.getUser_id())) throw new ApiException(ErrorCode.USER_NOT_FOUND);
        var reviews = feedbackRepository.findReceivedPage(actor.getUser_id(), PageRequest.of(page, size));
        Double average = feedbackRepository.averageRating(actor.getUser_id());
        double rating = average == null ? 0.0 : Math.round(average * 10.0) / 10.0;
        return new FeedbackApiResponse.Received(rating, reviews.getTotalElements(), reviews.getContent().stream().map(f ->
                new FeedbackApiResponse.ReviewItem(f.getFeedback_id(),
                        new FeedbackApiResponse.Reviewer(f.getWriter().getUser_id(), f.getWriter().getNickname()),
                        new FeedbackApiResponse.ProjectInfo(f.getProject().getProject_id(), f.getProject().getProject_title()),
                        f.getRating(), f.getFeedback_content(), f.getCreatedAt())).toList(), page, size, reviews.getTotalElements());
    }

    @Transactional(readOnly = true)
    public FeedbackApiResponse.Status status(User actor, Long projectId) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new ApiException(ErrorCode.PROJECT_NOT_FOUND));
        requirePm(actor, project, "평가 진행 상황 확인 권한이 없습니다.");
        var eligible = memberRepository.findMembers(projectId).stream().map(User::getUser_id)
                .filter(id -> !id.equals(project.getPm().getUser_id())).toList();
        Set<Long> reviewed = new HashSet<>(feedbackRepository.findEvaluatedMemberIds(projectId));
        return new FeedbackApiResponse.Status(projectId, project.getProject_status() == ProjectStatus.COMPLETED,
                eligible.stream().filter(reviewed::contains).toList(), eligible.stream().filter(id -> !reviewed.contains(id)).toList());
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> getFeedbacks(Long projectId) {
        if (!projectRepository.existsById(projectId)) throw new ApiException(ErrorCode.PROJECT_NOT_FOUND);
        return feedbackRepository.findByProjectId(projectId).stream().map(FeedbackResponse::from).toList();
    }

    private void requirePm(User actor, Project project, String message) {
        if (!project.getPm().getUser_id().equals(actor.getUser_id())) throw new ApiException(ErrorCode.NOT_PROJECT_PM, message);
    }
}
