package com.lastcalleats.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * This DTO is used to receive merchant registration data from the client.
 * It contains both account credentials and merchant profile information.
 *
 * The class is designed specifically for merchant registration because merchants
 * require additional fields such as business name and address.
 */
@Data
public class MerchantRegisterRequest {

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    private String password;

    @NotBlank(message = "Name cannot be blank")
    private String name;

    @NotBlank(message = "Address cannot be blank")
    private String address;
}