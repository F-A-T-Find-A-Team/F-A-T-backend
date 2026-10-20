package com.F_A_T.F_A_T.domain.project.controller;

import com.F_A_T.F_A_T.domain.project.dto.request.ProjectCreateRequest;
import com.F_A_T.F_A_T.domain.project.dto.request.ProjectStatusUpdateRequest;
import com.F_A_T.F_A_T.domain.project.dto.response.ProjectListResponse;
import com.F_A_T.F_A_T.domain.project.dto.response.ProjectMemberResponse;
import com.F_A_T.F_A_T.domain.project.dto.response.ProjectResponse;
import com.F_A_T.F_A_T.domain.project.service.ProjectService;
import com.F_A_T.F_A_T.domain.user.security.CustomUserDetails;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<Long> createProject(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid ProjectCreateRequest request) {

        Long projectId = projectService.createProject(userDetails.getUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(projectId);
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(projectService.getProject(projectId));
    }

    @PatchMapping("/{projectId}/status")
    public ResponseEntity<Void> updateStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId,
            @RequestBody @Valid ProjectStatusUpdateRequest request) {

        projectService.updateStatus(userDetails.getUser(), projectId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{projectId}/members")
    public ResponseEntity<List<ProjectMemberResponse>> getMembers(@PathVariable Long projectId) {
        return ResponseEntity.ok(projectService.getMembers(projectId));
    }

    @GetMapping
    public ResponseEntity<List<ProjectListResponse>> getProjectList() {
        return ResponseEntity.ok(projectService.getProjectList());
    }
}
