package com.lastcalleats.user.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Response DTO for a user's profile, returned to the frontend after profile reads and updates.
 * Excludes sensitive fields such as the password hash; built from a UserDO entity via the service layer.
 */
@Getter
@Builder
public class UserProfileResponse {

    private Long id;
    private String email;
    private String nickname;
    private String avatarUrl;   // URL of the user's uploaded avatar image; null if not yet set
}
