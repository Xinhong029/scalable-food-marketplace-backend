package com.lastcalleats.user.service;

import com.lastcalleats.user.dto.UserProfileRequest;
import com.lastcalleats.user.dto.UserProfileResponse;
import org.springframework.web.multipart.MultipartFile;
import com.lastcalleats.common.exception.BusinessException;

/**
 * Service interface for user profile operations.
 * Include methods to get, update profile and upload avatar image.
 */
public interface UserService {

    /**
     * Find user profile by user ID.
     *
     * @param userId the ID of the user to look up
     * @return response DTO with user profile data
     * @throws BusinessException if user not found
     */
    UserProfileResponse getProfile(Long userId);

    /**
     * Update user nickname and save.
     *
     * @param userId  the ID of the user to update
     * @param request contains new nickname value
     * @return response DTO with updated profile
     * @throws BusinessException if user not found
     */
    UserProfileResponse updateProfile(Long userId, UserProfileRequest request);

    // V2 reserved: changePassword

    /**
     * Upload avatar image for user and save the URL.
     * File is saved via StorageStrategy, result URL is stored in user record.
     *
     * @param userId the ID of the user
     * @param file   the image file from the request
     * @return response DTO with updated profile including avatar URL
     * @throws BusinessException if user not found
     */
    UserProfileResponse uploadAvatar(Long userId, MultipartFile file);
}
