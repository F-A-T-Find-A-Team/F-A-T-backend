package com.F_A_T.F_A_T.domain.mypage.controller;

import com.F_A_T.F_A_T.domain.mypage.dto.request.MyPageUpdateRequest;
import com.F_A_T.F_A_T.domain.mypage.dto.response.MyPageResponse;
import com.F_A_T.F_A_T.domain.mypage.service.MyPageService;
import com.F_A_T.F_A_T.domain.user.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mypage")
public class MyPageController {

    private final MyPageService myPageService;

    @GetMapping
    public ResponseEntity<MyPageResponse> getMyPage(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(myPageService.getMyPage(userDetails.getUser()));
    }

    @PatchMapping
    public ResponseEntity<Void> updateMyPage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid MyPageUpdateRequest request) {

        myPageService.updateMyPage(userDetails.getUser(), request);
        return ResponseEntity.ok().build();
    }
}