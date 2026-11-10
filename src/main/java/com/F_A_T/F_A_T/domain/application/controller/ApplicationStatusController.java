package com.F_A_T.F_A_T.domain.application.controller;

import com.F_A_T.F_A_T.domain.application.dto.request.ApplicationDecisionRequest;
import com.F_A_T.F_A_T.domain.application.dto.response.ApplicationApiResponse;
import com.F_A_T.F_A_T.domain.application.service.ProjectApplicationService;
import com.F_A_T.F_A_T.global.dto.ApiResponse;
import com.F_A_T.F_A_T.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/applications", "/api/v1/applications"})
public class ApplicationStatusController {
    private final ProjectApplicationService service;
    @PatchMapping("/{applicationId}")
    public ApiResponse<ApplicationApiResponse.Decision> decide(@AuthenticationPrincipal CustomUserDetails actor,
            @PathVariable Long applicationId, @RequestBody ApplicationDecisionRequest request) {
        return ApiResponse.ok(service.decide(actor.getUser(), applicationId, request));
    }
    @DeleteMapping("/{applicationId}")
    public ResponseEntity<Void> cancel(@AuthenticationPrincipal CustomUserDetails actor, @PathVariable Long applicationId) {
        service.cancel(actor.getUser(), applicationId);
        return ResponseEntity.noContent().build();
    }
}
