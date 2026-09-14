package com.lastcalleats.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for updating a user's profile via PUT /api/user/profile.
 * Contains only the fields that a user is allowed to modify; immutable fields such as email are excluded.
 */
@Data
public class UserProfileRequest {

    // Display name shown to other users; must be 2–50 characters
    @NotBlank
    @Size(min = 2, max = 50, message = "Nickname must be between 2 and 50 characters")
    private String nickname;
}
