package com.F_A_T.F_A_T.domain.application.controller;

import com.F_A_T.F_A_T.domain.application.dto.request.ApplicationCreateRequest;
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
@RequestMapping({"/projects/{projectId}/applications", "/api/v1/projects/{projectId}/applications"})
public class ProjectApplicationController {
    private final ProjectApplicationService service;
    @PostMapping
    public ResponseEntity<ApiResponse<ApplicationApiResponse.Created>> create(@AuthenticationPrincipal CustomUserDetails actor,
            @PathVariable Long projectId, @RequestBody ApplicationCreateRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.ok(service.create(actor.getUser(), projectId, request)));
    }
    @GetMapping
    public ApiResponse<ApplicationApiResponse.Page<ApplicationApiResponse.ApplicantItem>> list(@AuthenticationPrincipal CustomUserDetails actor,
            @PathVariable Long projectId, @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.projectApplications(actor.getUser(), projectId, status, page, size));
    }
}
