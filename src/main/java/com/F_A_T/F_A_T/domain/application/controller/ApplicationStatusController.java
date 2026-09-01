package com.F_A_T.F_A_T.domain.application.controller;

import com.F_A_T.F_A_T.domain.application.dto.request.ApplicationStatusUpdateRequest;
import com.F_A_T.F_A_T.domain.application.service.ProjectApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/applications")
public class ApplicationStatusController {

    private final ProjectApplicationService applicationService;

    @PatchMapping("/{applicationId}")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Long applicationId,
            @RequestBody @Valid ApplicationStatusUpdateRequest request) {

        applicationService.updateStatus(applicationId, request);
        return ResponseEntity.ok().build();
    }
}