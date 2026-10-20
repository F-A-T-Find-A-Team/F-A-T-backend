package com.F_A_T.F_A_T.domain.application.controller;

import com.F_A_T.F_A_T.domain.application.dto.request.ApplicationStatusUpdateRequest;
import com.F_A_T.F_A_T.domain.application.service.ProjectApplicationService;
import com.F_A_T.F_A_T.domain.user.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/applications")
public class ApplicationStatusController {

    private final ProjectApplicationService applicationService;

    @PatchMapping("/{applicationId}")
    public ResponseEntity<Void> updateStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long applicationId,
            @RequestBody @Valid ApplicationStatusUpdateRequest request) {

        applicationService.updateStatus(userDetails.getUser(), applicationId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{applicationId}")
    public ResponseEntity<Void> cancel(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long applicationId) {
        applicationService.cancel(userDetails.getUser(), applicationId);
        return ResponseEntity.noContent().build();
    }
}
