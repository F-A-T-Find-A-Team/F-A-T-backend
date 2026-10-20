package com.F_A_T.F_A_T.domain.user.service;

import com.F_A_T.F_A_T.domain.user.dto.request.SignupRequest;
import com.F_A_T.F_A_T.domain.user.entity.User;
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
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
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
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));

        if (!passwordEncoder.matches(rawPassword, user.getUser_password())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return user;
    }
}