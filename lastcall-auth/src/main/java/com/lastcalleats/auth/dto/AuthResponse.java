package com.lastcalleats.auth.dto;

import lombok.Data;

/**
 * This DTO represents the authentication result returned to the client after
 * a successful registration or login operation.
 *
 * It contains the generated JWT token and basic account information that the client
 * may need for subsequent authenticated requests.
 */
@Data
public class AuthResponse {

    private String accessToken;
    private String tokenType;
    private Long expiresIn;
    private Long userId;
    private String email;
    private String role;
}