package com.F_A_T.F_A_T.domain.mypage.service;

import com.F_A_T.F_A_T.domain.mypage.dto.request.MyPageUpdateRequest;
import com.F_A_T.F_A_T.domain.mypage.dto.response.MyPageResponse;
import com.F_A_T.F_A_T.domain.user.entity.User;
import com.F_A_T.F_A_T.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MyPageService {
    private final UserRepository userRepository;

    private User managed(User user) {
        return userRepository.findById(user.getUser_id()).orElseThrow();
    }

    @Transactional(readOnly = true)
    public MyPageResponse getMyPage(User user) {
        return MyPageResponse.from(managed(user));
    }

    @Transactional
    public void updateMyPage(User user, MyPageUpdateRequest request) {
        user = managed(user);
        if (request.userMajor() != null) {
            user.setUser_major(request.userMajor());
        }
        if (request.interestStacks() != null) {
            user.setInterest_stacks(request.interestStacks());
        }
        if (request.bio() != null) {
            user.setUser_bio(request.bio());
        }
        if (request.clubName() != null) {
            user.setClub_name(request.clubName());
        }
    }

}
