package com.F_A_T.F_A_T.domain.project.controller;

import com.F_A_T.F_A_T.domain.project.dto.request.*;
import com.F_A_T.F_A_T.domain.project.dto.response.ProjectApiResponse;
import com.F_A_T.F_A_T.domain.project.service.ProjectService;
import com.F_A_T.F_A_T.global.dto.ApiResponse;
import com.F_A_T.F_A_T.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/projects", "/api/v1/projects"})
public class ProjectController {
    private final ProjectService service;

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectApiResponse.Created>> create(@AuthenticationPrincipal CustomUserDetails actor,
            @RequestBody ProjectCreateRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.ok(service.create(actor.getUser(), request)));
    }
    @GetMapping
    public ApiResponse<ProjectApiResponse.Page<ProjectApiResponse.Card>> search(
            @RequestParam(required = false) String query, @RequestParam(required = false) String recruitmentStatus,
            @RequestParam(required = false) String major, @RequestParam(required = false) String techStack,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.search(query, recruitmentStatus, major, techStack, page, size));
    }
    @GetMapping("/recommended")
    public ApiResponse<ProjectApiResponse.Recommended> recommended(@AuthenticationPrincipal CustomUserDetails actor,
            @RequestParam(defaultValue = "5") int size) {
        return ApiResponse.ok(service.recommended(actor.getUser(), size));
    }
    @GetMapping("/{projectId}")
    public ApiResponse<ProjectApiResponse.Detail> detail(@AuthenticationPrincipal CustomUserDetails actor,
            @PathVariable Long projectId) {
        return ApiResponse.ok(service.detail(actor.getUser(), projectId));
    }
    @PatchMapping("/{projectId}")
    public ApiResponse<ProjectApiResponse.Updated> update(@AuthenticationPrincipal CustomUserDetails actor,
            @PathVariable Long projectId, @RequestBody ProjectUpdateRequest request) {
        return ApiResponse.ok(service.update(actor.getUser(), projectId, request));
    }
    @PatchMapping("/{projectId}/stage")
    public ApiResponse<ProjectApiResponse.StageChanged> stage(@AuthenticationPrincipal CustomUserDetails actor,
            @PathVariable Long projectId, @RequestBody ProjectStageRequest request) {
        return ApiResponse.ok(service.changeStage(actor.getUser(), projectId, request));
    }
    @GetMapping("/{projectId}/members")
    public ApiResponse<ProjectApiResponse.Members> members(@PathVariable Long projectId) {
        return ApiResponse.ok(service.members(projectId));
    }
}
