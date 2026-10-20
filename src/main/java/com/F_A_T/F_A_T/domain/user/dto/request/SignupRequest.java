package com.F_A_T.F_A_T.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SignupRequest(
        @Email @NotBlank String userEmail,
        @NotBlank String userPassword,
        @NotBlank String userGender,
        @NotBlank String userMajor,
        List<String> interestStacks,
        @NotNull Integer userStudentNumber,
        @NotBlank String userName
) {}