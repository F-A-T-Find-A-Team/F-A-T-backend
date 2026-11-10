package com.F_A_T.F_A_T.domain.auth.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public final class AuthRequests {
    private AuthRequests() {}
    public record Email(@NotBlank @jakarta.validation.constraints.Email String email, @NotBlank @Pattern(regexp="SIGNUP|VERIFY_ACCOUNT") String purpose, String password) {}
    public record Verify(@NotBlank String verificationId, @NotBlank @Pattern(regexp="[0-9]{6}") String code) {}
    public record Login(@NotBlank @jakarta.validation.constraints.Email String email, @NotBlank String password) {}
    public record Signup(@NotBlank String signupToken, @NotBlank @jakarta.validation.constraints.Email String email,
            @NotBlank @Size(min=8,max=72) String password, @NotBlank @Size(max=40) String nickname,
            @NotBlank @Pattern(regexp="[0-9]{4,9}") String studentId, @NotBlank @Size(max=100) String name,
            @NotBlank @Pattern(regexp="MALE|FEMALE|OTHER") String gender, @Size(max=100) String bio,
            @NotEmpty @Size(max=20) List<@NotBlank @Size(max=100) String> majors,
            @NotEmpty @Size(max=50) List<@NotBlank @Size(max=100) String> techStacks, String avatarFileId) {}
}
