package com.lastcalleats.user.service.impl;

import com.lastcalleats.common.exception.BusinessException;
import com.lastcalleats.common.exception.ErrorCode;
import com.lastcalleats.common.storage.StorageStrategy;
import com.lastcalleats.user.dto.UserProfileRequest;
import com.lastcalleats.user.dto.UserProfileResponse;
import com.lastcalleats.user.entity.UserDO;
import com.lastcalleats.user.repository.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserServiceImpl.
 * Cover getProfile, updateProfile, and uploadAvatar with both success and not-found cases.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private StorageStrategy storageStrategy;

    @InjectMocks
    private UserServiceImpl userService;

    private UserDO testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserDO();
        testUser.setId(1L);
        testUser.setEmail("user@example.com");
        testUser.setNickname("Alice");
        testUser.setAvatarUrl("http://example.com/avatar.jpg");
    }

    @Test
    void getProfile_userExists_returnsProfile() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));

        UserProfileResponse response = userService.getProfile(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("user@example.com", response.getEmail());
        assertEquals("Alice", response.getNickname());
        assertEquals("http://example.com/avatar.jpg", response.getAvatarUrl());
        verify(userRepo).findById(1L);
    }

    @Test
    void getProfile_userNotFound_throwsException() {
        when(userRepo.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.getProfile(99L));
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void updateProfile_validRequest_updatesNickname() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepo.save(any(UserDO.class))).thenReturn(testUser);

        UserProfileRequest request = new UserProfileRequest();
        request.setNickname("Bob");

        UserProfileResponse response = userService.updateProfile(1L, request);

        assertEquals("Bob", testUser.getNickname());
        assertNotNull(response);
        verify(userRepo).save(testUser);
    }

    @Test
    void updateProfile_userNotFound_throwsException() {
        when(userRepo.findById(99L)).thenReturn(Optional.empty());

        UserProfileRequest request = new UserProfileRequest();
        request.setNickname("Bob");

        assertThrows(BusinessException.class, () -> userService.updateProfile(99L, request));
        verify(userRepo, never()).save(any());
    }

    @Test
    void uploadAvatar_success_updatesAvatarUrl() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));
        when(storageStrategy.upload(any(), eq("avatars"))).thenReturn("/uploads/avatars/new.jpg");
        when(userRepo.save(any(UserDO.class))).thenReturn(testUser);

        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg",
                "image/jpeg", new byte[]{1, 2, 3});
        UserProfileResponse response = userService.uploadAvatar(1L, file);

        assertEquals("/uploads/avatars/new.jpg", testUser.getAvatarUrl());
        assertNotNull(response);
        verify(userRepo).save(testUser);
    }

    @Test
    void uploadAvatar_userNotFound_throwsException() {
        when(userRepo.findById(99L)).thenReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg",
                "image/jpeg", new byte[]{1, 2, 3});

        assertThrows(BusinessException.class, () -> userService.uploadAvatar(99L, file));
        verify(storageStrategy, never()).upload(any(), any());
    }
}
