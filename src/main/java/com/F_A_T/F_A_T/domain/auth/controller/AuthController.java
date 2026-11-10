package com.F_A_T.F_A_T.domain.auth.controller;

import com.F_A_T.F_A_T.domain.auth.dto.AuthRequests;
import com.F_A_T.F_A_T.domain.auth.service.AuthService;
import com.F_A_T.F_A_T.global.dto.ApiResponse;
import com.F_A_T.F_A_T.global.security.*;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService auth;
    private final boolean secure;
    public AuthController(AuthService auth,@Value("${app.auth.secure-cookie:true}") boolean secure) {
        this.auth=auth;this.secure=secure;
    }
    @PostMapping("/email/send") public ApiResponse<?> send(@RequestBody @Valid AuthRequests.Email request) { return ApiResponse.ok(auth.send(request)); }
    @PostMapping("/email/verify") public ApiResponse<?> verify(@RequestBody @Valid AuthRequests.Verify request) { return ApiResponse.ok(auth.verify(request)); }
    @PostMapping("/signup") public ResponseEntity<?> signup(@RequestBody @Valid AuthRequests.Signup request) {
        return ResponseEntity.status(201).body(ApiResponse.ok(auth.signup(request)));
    }
    @PostMapping("/login") public ResponseEntity<?> login(@RequestBody @Valid AuthRequests.Login request) { return session(auth.login(request)); }
    @PostMapping("/refresh") public ResponseEntity<?> refresh(@CookieValue(value="refreshToken",required=false) String refresh) { return session(auth.refresh(refresh)); }
    @PostMapping("/logout") public ResponseEntity<Void> logout(@AuthenticationPrincipal CustomUserDetails principal) {
        auth.logout(principal.getUser().getUser_id(),principal.getSessionId());
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE,cookie("",0)).build();
    }
    private ResponseEntity<?> session(AuthService.SessionResponse session) {
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,cookie(session.refreshToken(),session.refreshSeconds())).body(ApiResponse.ok(session.data()));
    }
    private String cookie(String token,long seconds) {
        return ResponseCookie.from("refreshToken",token).httpOnly(true).secure(secure).sameSite("Strict")
                .path("/auth").maxAge(seconds).build().toString();
    }
}
