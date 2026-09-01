package com.F_A_T.F_A_T.domain.application.controller;

import com.F_A_T.F_A_T.domain.application.dto.request.ApplicationCreateRequest;
import com.F_A_T.F_A_T.domain.application.service.ProjectApplicationService;
import com.F_A_T.F_A_T.domain.user.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/projects/{projectId}/applications")
public class ProjectApplicationController {

    private final ProjectApplicationService applicationService;

    @PostMapping
    public ResponseEntity<Long> apply(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId,
            @RequestBody @Valid ApplicationCreateRequest request) {

        Long applicationId = applicationService.apply(userDetails.getUser(), projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(applicationId);
    }
}