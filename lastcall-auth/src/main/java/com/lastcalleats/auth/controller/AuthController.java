package com.lastcalleats.auth.controller;

import com.lastcalleats.auth.dto.AuthResponse;
import com.lastcalleats.auth.dto.LoginRequest;
import com.lastcalleats.auth.dto.MerchantRegisterRequest;
import com.lastcalleats.auth.dto.UserRegisterRequest;
import com.lastcalleats.auth.service.AuthService;
import com.lastcalleats.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * This controller handles authentication-related HTTP requests for the system.
 * It provides endpoints for user registration, merchant registration, and login operations.
 *
 * The controller is responsible for receiving request data from clients and delegating
 * the actual business logic to the service layer. It does not contain core authentication
 * logic itself, which helps keep the application structure clear and maintainable.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  /**
   * Registers a new user account and returns an authentication response.
   *
   * This endpoint accepts user registration data, forwards it to the service layer,
   * and returns a token together with basic account information after successful registration.
   *
   * @param request the user registration request containing email, password, and nickname
   * @return a successful API response containing authentication information for the new user
   */
  @PostMapping("/register/user")
  public ResponseEntity<ApiResponse<AuthResponse>> registerUser(
      @Valid @RequestBody UserRegisterRequest request) {
    AuthResponse response = authService.registerUser(request);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * Registers a new merchant account and returns an authentication response.
   *
   * This endpoint is similar to user registration, but it is intended for merchant accounts.
   * After the merchant is saved successfully, the system generates a JWT token for later access.
   *
   * @param request the merchant registration request containing email, password, name, and address
   * @return a successful API response containing authentication information for the new merchant
   */
  @PostMapping("/register/merchant")
  public ResponseEntity<ApiResponse<AuthResponse>> registerMerchant(
      @Valid @RequestBody MerchantRegisterRequest request) {
    AuthResponse response = authService.registerMerchant(request);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * Authenticates a user account and returns a JWT-based authentication response.
   *
   * This method validates the login request through the service layer and returns a token
   * if the provided credentials are correct.
   *
   * @param request the login request containing user email and password
   * @return a successful API response containing authentication data for the user
   */
  @PostMapping("/login/user")
  public ResponseEntity<ApiResponse<AuthResponse>> loginUser(
      @Valid @RequestBody LoginRequest request) {
    AuthResponse response = authService.login(request, "USER");
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * Authenticates a merchant account and returns a JWT-based authentication response.
   *
   * This method verifies merchant credentials and generates an authentication token
   * that can be used in subsequent protected API requests.
   *
   * @param request the login request containing merchant email and password
   * @return a successful API response containing authentication data for the merchant
   */
  @PostMapping("/login/merchant")
  public ResponseEntity<ApiResponse<AuthResponse>> loginMerchant(
      @Valid @RequestBody LoginRequest request) {
    AuthResponse response = authService.login(request, "MERCHANT");
    return ResponseEntity.ok(ApiResponse.success(response));
  }
}