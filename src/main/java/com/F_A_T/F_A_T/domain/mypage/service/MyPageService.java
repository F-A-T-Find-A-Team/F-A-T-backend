package com.F_A_T.F_A_T.domain.mypage.service;

import com.F_A_T.F_A_T.domain.mypage.dto.request.MyPageUpdateRequest;
import com.F_A_T.F_A_T.domain.mypage.dto.request.ProfileUpdateRequest;
import com.F_A_T.F_A_T.domain.mypage.dto.response.MyPageResponse;
import com.F_A_T.F_A_T.domain.mypage.dto.response.ProfileResponse;
import com.F_A_T.F_A_T.domain.mypage.dto.response.ProjectHistoryResponse;
import com.F_A_T.F_A_T.domain.mypage.repository.ProfileStatisticsRepository;
import com.F_A_T.F_A_T.domain.mypage.storage.AvatarStorage;
import com.F_A_T.F_A_T.domain.project.entity.ProjectStatus;
import com.F_A_T.F_A_T.domain.user.entity.User;
import com.F_A_T.F_A_T.domain.user.repository.UserRepository;
import com.F_A_T.F_A_T.global.exception.ApiException;
import com.F_A_T.F_A_T.global.exception.ErrorCode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class MyPageService {
    private final UserRepository userRepository;
    private final ProfileStatisticsRepository statistics;
    private final AvatarStorage avatarStorage;

    private User managed(User user) {
        return load(user.getUser_id());
    }
    private User load(Long id) {
        if (id == null || id <= 0) throw new ApiException(ErrorCode.INVALID_USER_ID);
        return userRepository.findById(id).orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(User actor) {
        User user = managed(actor);
        Long id = user.getUser_id();
        return ProfileResponse.from(user,
                new ProfileResponse.ProjectStats(statistics.projectCount(id, ProjectStatus.IN_PROGRESS), statistics.projectCount(id, ProjectStatus.COMPLETED)),
                new ProfileResponse.FeedbackStats(statistics.averageRating(id), statistics.feedbackCount(id)));
    }

    @Transactional(readOnly = true)
    public ProfileResponse.PublicProfile getPublicProfile(Long userId) {
        return ProfileResponse.publicFrom(load(userId));
    }

    @Transactional
    public ProfileResponse.UpdatedProfile updateProfile(User actor, ProfileUpdateRequest request) {
        User user = managed(actor);
        if (request.nickname() != null) {
            String nickname = request.nickname().trim();
            if (nickname.isBlank() || nickname.length() > 40) throw new ApiException(ErrorCode.INVALID_PROFILE_DATA);
            if (userRepository.nicknameExists(nickname, user.getUser_id())) {
                throw new ApiException(ErrorCode.NICKNAME_ALREADY_EXISTS);
            }
            user.setNickname(nickname);
        }
        if (request.gender() != null) {
            if (request.gender().isBlank() || request.gender().length() > 32) throw new ApiException(ErrorCode.INVALID_PROFILE_DATA);
            user.setUser_gender(request.gender().trim());
        }
        if (request.bio() != null) {
            if (request.bio().length() > 100) throw new ApiException(ErrorCode.INVALID_PROFILE_DATA);
            user.setUser_bio(request.bio());
        }
        if (request.majors() != null) user.setMajors(tags(request.majors(), true, 8));
        if (request.techStacks() != null) user.setInterest_stacks(tags(request.techStacks(), false, 40));
        try { userRepository.saveAndFlush(user); }
        catch (DataIntegrityViolationException ex) {
            throw new ApiException(request.nickname() != null ? ErrorCode.NICKNAME_ALREADY_EXISTS : ErrorCode.INVALID_PROFILE_DATA);
        }
        return ProfileResponse.updatedFrom(user);
    }

    private List<String> tags(List<String> values, boolean required, int maximum) {
        if ((required && values.isEmpty()) || values.size() > maximum) throw new ApiException(ErrorCode.INVALID_PROFILE_DATA);
        var seen = new HashSet<String>();
        var result = new ArrayList<String>();
        for (String value : values) {
            if (value == null || value.isBlank() || value.trim().length() > 64 || !seen.add(value.trim().toLowerCase(Locale.ROOT))) {
                throw new ApiException(ErrorCode.INVALID_PROFILE_DATA);
            }
            result.add(value.trim());
        }
        return result;
    }

    @Transactional
    public String uploadAvatar(User actor, MultipartFile file) {
        User user = managed(actor);
        String url = avatarStorage.save(file);
        user.setAvatar_url(url);
        return url;
    }

    @Transactional(readOnly = true)
    public ProjectHistoryResponse projectHistory(User actor, String stageValue, int page, int size) {
        managed(actor);
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) {
            throw new ApiException(ErrorCode.INVALID_PAGINATION);
        }
        ProjectStatus stage = null;
        if (stageValue != null) {
            try { stage = ProjectStatus.valueOf(stageValue); }
            catch (IllegalArgumentException ex) { throw new ApiException(ErrorCode.INVALID_STAGE); }
        }
        var projects = statistics.history(actor.getUser_id(), stage, page, size);
        var items = projects.stream().map(p -> new ProjectHistoryResponse.Item(p.getProject_id(),
                p.getProject_title(), p.getProject_status(), p.getPm().getUser_id().equals(actor.getUser_id()) ? "PM" : "MEMBER",
                statistics.memberCount(p.getProject_id()), p.getRecruit_limit())).toList();
        return new ProjectHistoryResponse(items, page, size, projects.getTotalElements(), projects.getTotalPages());
    }

    /** Existing service entry point retained for internal callers; HTTP uses the canonical profile DTO. */
    @Transactional(readOnly = true)
    public MyPageResponse getMyPage(User user) {
        return MyPageResponse.from(managed(user));
    }

    @Transactional
    public void updateMyPage(User actor, MyPageUpdateRequest request) {
        User user = managed(actor);
        if (request.userMajor() != null) user.setMajors(List.of(request.userMajor()));
        if (request.interestStacks() != null) user.setInterest_stacks(tags(request.interestStacks(), false, 40));
        if (request.bio() != null) {
            if (request.bio().length() > 100) throw new ApiException(ErrorCode.INVALID_PROFILE_DATA);
            user.setUser_bio(request.bio());
        }
        if (request.clubName() != null) user.setClub_name(request.clubName());
    }
}
