package com.F_A_T.F_A_T.profile;

import com.F_A_T.F_A_T.domain.mypage.dto.request.ProfileUpdateRequest;
import com.F_A_T.F_A_T.domain.mypage.repository.ProfileStatisticsRepository;
import com.F_A_T.F_A_T.domain.mypage.service.MyPageService;
import com.F_A_T.F_A_T.domain.mypage.storage.AvatarStorage;
import com.F_A_T.F_A_T.domain.user.entity.User;
import com.F_A_T.F_A_T.domain.user.repository.UserRepository;
import com.F_A_T.F_A_T.global.exception.ApiException;
import com.F_A_T.F_A_T.global.exception.ErrorCode;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfileServiceTests {
    UserRepository users;
    MyPageService service;
    User user;
    @BeforeEach void setUp() {
        users = mock(UserRepository.class);
        service = new MyPageService(users, mock(ProfileStatisticsRepository.class), mock(AvatarStorage.class));
        user = User.builder().user_name("Original Name").user_email("member@dsm.hs.kr").user_password("hash")
                .user_gender("MALE").user_major("BE").interest_stacks(List.of("Java"))
                .user_student_number(1107).build();
        user.setUser_id(7L);
        when(users.findById(7L)).thenReturn(Optional.of(user));
    }
    @Test void legacyIdentityAndMajorRemainReadableWithoutInventingVerification() {
        assertEquals("Original Name", user.getNickname());
        assertEquals("Original Name", user.getRealName());
        assertEquals("1107", user.getStudentId());
        assertEquals(List.of("BE"), user.getMajors());
        assertFalse(user.isEmailVerified());
        user.setNickname("new-nickname");
        assertEquals("Original Name", user.getRealName());
        assertEquals("new-nickname", user.getUser_name());
    }
    @Test void nicknameGenderAndCollectionsUpdateWithoutChangingPrivateIdentifiers() {
        var response = service.updateProfile(user, new ProfileUpdateRequest(" nickname ", "FEMALE", "bio", List.of("FE", "UI/UX"), List.of("Flutter")));
        assertEquals("nickname", response.nickname());
        assertEquals("FEMALE", response.gender());
        assertEquals(List.of("FE", "UI/UX"), response.majors());
        assertEquals("FE", user.getUser_major());
        assertEquals("member@dsm.hs.kr", user.getUser_email());
        assertEquals("Original Name", user.getRealName());
        verify(users).nicknameExists("nickname", 7L);
        verify(users).saveAndFlush(user);
    }
    @Test void partialPatchPreservesFieldsNotSent() {
        var response = service.updateProfile(user, new ProfileUpdateRequest(null, null, "updated", null, null));
        assertEquals("Original Name", response.nickname());
        assertEquals("MALE", response.gender());
        assertEquals(List.of("BE"), response.majors());
        assertEquals(List.of("Java"), response.techStacks());
        assertEquals("updated", response.bio());
    }
    @Test void duplicateNicknameFailsBeforeMutationOrWrite() {
        when(users.nicknameExists("taken", 7L)).thenReturn(true);
        var error = assertThrows(ApiException.class, () -> service.updateProfile(user, new ProfileUpdateRequest("taken", null, null, null, null)));
        assertEquals(ErrorCode.NICKNAME_ALREADY_EXISTS, error.getErrorCode());
        assertEquals("Original Name", user.getNickname());
        verify(users, never()).saveAndFlush(any());
    }
    @Test void invalidTagsAndOverlongBioAreRejected() {
        assertEquals(ErrorCode.INVALID_PROFILE_DATA, assertThrows(ApiException.class,
                () -> service.updateProfile(user, new ProfileUpdateRequest(null, null, "x".repeat(101), null, null))).getErrorCode());
        assertEquals(ErrorCode.INVALID_PROFILE_DATA, assertThrows(ApiException.class,
                () -> service.updateProfile(user, new ProfileUpdateRequest(null, null, null, List.of("FE", " fe "), null))).getErrorCode());
        assertEquals(ErrorCode.INVALID_PROFILE_DATA, assertThrows(ApiException.class,
                () -> service.updateProfile(user, new ProfileUpdateRequest(null, null, null, List.of(), null))).getErrorCode());
    }
    @Test void publicProfileNeverContainsPrivateIdentityOrContactFields() {
        var response = service.getPublicProfile(7L);
        assertEquals("Original Name", response.nickname());
        var fields = Arrays.stream(response.getClass().getRecordComponents()).map(c -> c.getName()).toList();
        assertFalse(fields.contains("email"));
        assertFalse(fields.contains("studentId"));
        assertFalse(fields.contains("name"));
        assertFalse(fields.contains("password"));
        assertThrows(ApiException.class, () -> service.getPublicProfile(-1L));
    }
    @Test void invalidHistoryFiltersAreRejectedBeforeQuerying() {
        assertEquals(ErrorCode.INVALID_STAGE, assertThrows(ApiException.class,
                () -> service.projectHistory(user, "UNKNOWN", 0, 20)).getErrorCode());
        assertEquals(ErrorCode.INVALID_PAGINATION, assertThrows(ApiException.class,
                () -> service.projectHistory(user, null, -1, 20)).getErrorCode());
        assertEquals(ErrorCode.INVALID_PAGINATION, assertThrows(ApiException.class,
                () -> service.projectHistory(user, null, 0, 101)).getErrorCode());
    }
}
