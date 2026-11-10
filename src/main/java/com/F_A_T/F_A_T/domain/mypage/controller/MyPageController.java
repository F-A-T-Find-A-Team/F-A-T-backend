package com.F_A_T.F_A_T.domain.mypage.controller;

import com.F_A_T.F_A_T.domain.mypage.dto.request.ProfileUpdateRequest;
import com.F_A_T.F_A_T.domain.mypage.dto.response.ProfileResponse;
import com.F_A_T.F_A_T.domain.mypage.dto.response.ProjectHistoryResponse;
import com.F_A_T.F_A_T.domain.mypage.service.MyPageService;
import com.F_A_T.F_A_T.global.dto.ApiResponse;
import com.F_A_T.F_A_T.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class MyPageController {
    private final MyPageService service;

    @GetMapping("/me")
    public ApiResponse<ProfileResponse> getProfile(@AuthenticationPrincipal CustomUserDetails principal) {
        return ApiResponse.ok(service.getProfile(principal.getUser()));
    }

    @PatchMapping("/me")
    public ApiResponse<ProfileResponse.UpdatedProfile> updateProfile(
            @AuthenticationPrincipal CustomUserDetails principal, @RequestBody @Valid ProfileUpdateRequest request) {
        return ApiResponse.ok(service.updateProfile(principal.getUser(), request));
    }

    @PostMapping(value = "/me/avatar", consumes = "multipart/form-data")
    public ApiResponse<Map<String, String>> uploadAvatar(@AuthenticationPrincipal CustomUserDetails principal,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(Map.of("avatarUrl", service.uploadAvatar(principal.getUser(), file)));
    }

    @GetMapping("/me/projects")
    public ApiResponse<ProjectHistoryResponse> history(@AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) String stage,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.projectHistory(principal.getUser(), stage, page, size));
    }

    @GetMapping("/{userId}")
    public ApiResponse<ProfileResponse.PublicProfile> publicProfile(@PathVariable Long userId) {
        return ApiResponse.ok(service.getPublicProfile(userId));
    }
}
