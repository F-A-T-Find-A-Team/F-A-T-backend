package com.F_A_T.F_A_T.domain.feedback.controller;

import com.F_A_T.F_A_T.domain.feedback.dto.request.FeedbackCreateRequest;
import com.F_A_T.F_A_T.domain.feedback.dto.response.FeedbackResponse;
import com.F_A_T.F_A_T.domain.feedback.service.FeedbackService;
import com.F_A_T.F_A_T.domain.user.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/projects/{projectId}/feedbacks")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public ResponseEntity<Long> createFeedback(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId,
            @RequestBody @Valid FeedbackCreateRequest request) {

        Long feedbackId = feedbackService.createFeedback(userDetails.getUser(), projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(feedbackId);
    }

    @GetMapping
    public ResponseEntity<List<FeedbackResponse>> getFeedbacks(@PathVariable Long projectId) {
        return ResponseEntity.ok(feedbackService.getFeedbacks(projectId));
    }
}