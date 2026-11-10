package com.F_A_T.F_A_T.domain.application.controller;

import com.F_A_T.F_A_T.domain.application.dto.response.ApplicationApiResponse;
import com.F_A_T.F_A_T.domain.application.service.ProjectApplicationService;
import com.F_A_T.F_A_T.global.dto.ApiResponse;
import com.F_A_T.F_A_T.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/users/me/applications", "/api/v1/users/me/applications"})
public class MyApplicationsController {
    private final ProjectApplicationService service;
    @GetMapping
    public ApiResponse<ApplicationApiResponse.Page<ApplicationApiResponse.MineItem>> mine(@AuthenticationPrincipal CustomUserDetails actor,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.mine(actor.getUser(), page, size));
    }
}
