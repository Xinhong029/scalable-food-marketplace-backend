package com.lastcalleats.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * This DTO is used to receive user registration data from the client.
 * It contains the information required to create a new user account.
 *
 * The class is separate from merchant registration because a normal user
 * has different profile data and registration requirements.
 */
@Data
public class UserRegisterRequest {

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    private String password;

    @NotBlank(message = "Nickname cannot be blank")
    private String nickname;
}