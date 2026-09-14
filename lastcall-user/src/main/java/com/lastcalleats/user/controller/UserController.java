package com.lastcalleats.user.controller;

import com.lastcalleats.common.response.ApiResponse;
import com.lastcalleats.user.dto.FavoriteListingResponse;
import com.lastcalleats.user.dto.UserProfileRequest;
import com.lastcalleats.user.dto.UserProfileResponse;
import com.lastcalleats.user.service.FavoriteService;
import com.lastcalleats.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST controller for user-related operations.
 * Handle profile management, avatar upload, and favorite listing endpoints.
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final FavoriteService favoriteService;

    /**
     * Returns the authenticated user's profile information.
     *
     * @return the profile data wrapped in a standard API response
     */
    @GetMapping("/profile")
    public ApiResponse<UserProfileResponse> getProfile() {
        Long userId = getCurrentUserId();
        return ApiResponse.success(userService.getProfile(userId));
    }

    /**
     * Updates the authenticated user's profile with the provided data.
     *
     * @param request the profile fields to update (e.g. nickname); validated before processing
     * @return the updated profile wrapped in a standard API response
     */
    @PutMapping("/profile")
    public ApiResponse<UserProfileResponse> updateProfile(
            @Valid @RequestBody UserProfileRequest request) {
        Long userId = getCurrentUserId();
        return ApiResponse.success(userService.updateProfile(userId, request));
    }

    /**
     * Uploads a new avatar image for the authenticated user.
     *
     * @param file the image file to upload, supplied as multipart form data
     * @return the updated profile including the new avatar URL
     */
    @PostMapping("/avatar")
    public ApiResponse<UserProfileResponse> uploadAvatar(
            @RequestParam("file") MultipartFile file) {
        Long userId = getCurrentUserId();
        return ApiResponse.success(userService.uploadAvatar(userId, file));
    }

    /**
     * Returns all product listings that the authenticated user has favourited.
     *
     * @return a list of enriched favourite listing details wrapped in a standard API response
     */
    @GetMapping("/favorites")
    public ApiResponse<List<FavoriteListingResponse>> listFavorites() {
        Long userId = getCurrentUserId();
        return ApiResponse.success(favoriteService.listFavorites(userId));
    }

    /**
     * Adds the specified product listing to the authenticated user's favourites.
     *
     * @param listingId the ID of the listing to favourite
     * @return an empty success response
     */
    @PostMapping("/favorites/{listingId}")
    public ApiResponse<Void> addFavorite(@PathVariable Long listingId) {
        Long userId = getCurrentUserId();
        favoriteService.addFavorite(userId, listingId);
        return ApiResponse.success();
    }

    /**
     * Removes the specified listing from the authenticated user's favourites.
     *
     * @param listingId the ID of the listing to un-favourite
     * @return an empty success response
     */
    @DeleteMapping("/favorites/{listingId}")
    public ApiResponse<Void> removeFavorite(@PathVariable Long listingId) {
        Long userId = getCurrentUserId();
        favoriteService.removeFavorite(userId, listingId);
        return ApiResponse.success();
    }

    /**
     * Checks whether the authenticated user has favourited the given listing.
     *
     * @param listingId the ID of the listing to check
     * @return true if favorited, false otherwise
     */
    @GetMapping("/favorites/{listingId}")
    public ApiResponse<Boolean> isFavorite(@PathVariable Long listingId) {
        Long userId = getCurrentUserId();
        return ApiResponse.success(favoriteService.isFavorite(userId, listingId));
    }

    // Extract the authenticated user ID from the SecurityContext
    private Long getCurrentUserId() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return Long.parseLong(userId);
    }
}
