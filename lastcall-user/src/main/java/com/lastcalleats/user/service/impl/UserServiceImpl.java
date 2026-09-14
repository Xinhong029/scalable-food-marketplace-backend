package com.lastcalleats.user.service.impl;

import com.lastcalleats.common.exception.BusinessException;
import com.lastcalleats.common.exception.ErrorCode;
import com.lastcalleats.common.storage.StorageStrategy;
import com.lastcalleats.user.dto.UserProfileRequest;
import com.lastcalleats.user.dto.UserProfileResponse;
import com.lastcalleats.user.entity.UserDO;
import com.lastcalleats.user.repository.UserRepo;
import com.lastcalleats.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Implementation of UserService.
 * Get, update user profile and upload avatar image.
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepo userRepo;
    // handle file upload to local disk or cloud storage
    private final StorageStrategy storageStrategy;

    /**
     * Find user by ID and return profile data.
     *
     * @param userId the ID of the user to look up
     * @return response DTO with user profile data
     * @throws BusinessException if user not found
     */
    @Override
    public UserProfileResponse getProfile(Long userId) {
        UserDO user = findUserById(userId);
        return toResponse(user);
    }

    /**
     * Update user nickname and save to database.
     *
     * @param userId  the ID of the user to update
     * @param request contains the new nickname
     * @return response DTO with updated profile
     * @throws BusinessException if user not found
     */
    @Override
    public UserProfileResponse updateProfile(Long userId, UserProfileRequest request) {
        UserDO user = findUserById(userId);
        user.setNickname(request.getNickname());
        userRepo.save(user);
        return toResponse(user);
    }

    /**
     * Upload avatar image and save the URL to the user record.
     * Use StorageStrategy so we can switch storage backend without changing this class.
     *
     * @param userId the ID of the user uploading the avatar
     * @param file   the image file from the request
     * @return response DTO with updated profile including new avatar URL
     * @throws BusinessException if user not found
     */
    @Override
    public UserProfileResponse uploadAvatar(Long userId, MultipartFile file) {
        UserDO user = findUserById(userId);
        // call storage strategy to save file and get back the URL
        String avatarUrl = storageStrategy.upload(file, "avatars");
        user.setAvatarUrl(avatarUrl);
        userRepo.save(user);
        return toResponse(user);
    }

    // find user by ID, throw BusinessException if not found
    private UserDO findUserById(Long userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    // build response DTO from UserDO
    private UserProfileResponse toResponse(UserDO user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}
