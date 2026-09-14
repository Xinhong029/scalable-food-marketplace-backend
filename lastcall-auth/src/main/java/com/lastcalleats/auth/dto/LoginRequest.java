package com.lastcalleats.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * This DTO is used to receive login data from the client.
 * It contains the minimum information required to authenticate an account.
 *
 * The request is shared by both normal users and merchants because both roles
 * log in using the same credential format: email and password.
 */
@Data
public class LoginRequest {

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    private String password;
}