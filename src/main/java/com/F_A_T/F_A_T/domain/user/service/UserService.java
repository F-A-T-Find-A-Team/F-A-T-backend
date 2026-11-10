package com.F_A_T.F_A_T.domain.user.service;

import com.F_A_T.F_A_T.domain.user.dto.request.SignupRequest;
import com.F_A_T.F_A_T.domain.user.entity.User;
import com.F_A_T.F_A_T.global.exception.*;
import com.F_A_T.F_A_T.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long signup(SignupRequest request) {
        if (userRepository.existsByUserEmail(request.userEmail())) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.builder()
                .user_email(request.userEmail())
                .user_password(passwordEncoder.encode(request.userPassword())) // 암호화 저장
                .user_gender(request.userGender())
                .user_major(request.userMajor())
                .interest_stacks(request.interestStacks())
                .user_student_number(request.userStudentNumber())
                .user_name(request.userName())
                .build();

        return userRepository.save(user).getUser_id();
    }

    public User login(String email, String rawPassword) {
        User user = userRepository.findByUserEmail(email)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(rawPassword, user.getUser_password())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        return user;
    }
}