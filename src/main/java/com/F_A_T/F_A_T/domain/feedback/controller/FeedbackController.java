package com.F_A_T.F_A_T.domain.feedback.controller;

import com.F_A_T.F_A_T.domain.feedback.dto.request.FeedbackSubmissionRequest;
import com.F_A_T.F_A_T.domain.feedback.dto.response.FeedbackApiResponse;
import com.F_A_T.F_A_T.domain.feedback.service.FeedbackService;
import com.F_A_T.F_A_T.global.dto.ApiResponse;
import com.F_A_T.F_A_T.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class FeedbackController {
    private final FeedbackService feedbackService;

    @PostMapping("/projects/{projectId}/feedback")
    public ResponseEntity<ApiResponse<FeedbackApiResponse.Submitted>> submit(@AuthenticationPrincipal CustomUserDetails actor,
            @PathVariable Long projectId, @RequestBody FeedbackSubmissionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(feedbackService.submit(actor.getUser(), projectId, request)));
    }

    @GetMapping("/users/me/feedback")
    public ApiResponse<FeedbackApiResponse.Received> received(@AuthenticationPrincipal CustomUserDetails actor,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(feedbackService.received(actor.getUser(), page, size));
    }

    @GetMapping("/projects/{projectId}/feedback-status")
    public ApiResponse<FeedbackApiResponse.Status> status(@AuthenticationPrincipal CustomUserDetails actor, @PathVariable Long projectId) {
        return ApiResponse.ok(feedbackService.status(actor.getUser(), projectId));
    }
}
