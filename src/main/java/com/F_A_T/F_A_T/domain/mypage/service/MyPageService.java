package com.F_A_T.F_A_T.domain.mypage.service;

import com.F_A_T.F_A_T.domain.mypage.dto.request.MyPageUpdateRequest;
import com.F_A_T.F_A_T.domain.mypage.dto.response.MyPageResponse;
import com.F_A_T.F_A_T.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MyPageService {

    @Transactional(readOnly = true)
    public MyPageResponse getMyPage(User user) {
        return MyPageResponse.from(user);
    }

    @Transactional
    public void updateMyPage(User user, MyPageUpdateRequest request) {
        if (request.userMajor() != null) {
            user.setUser_major(request.userMajor());
        }
        if (request.interestStacks() != null) {
            user.setInterest_stacks(request.interestStacks());
        }
    }
}