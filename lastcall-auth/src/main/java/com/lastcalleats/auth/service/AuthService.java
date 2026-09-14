package com.lastcalleats.auth.service;

import com.lastcalleats.auth.dto.AuthResponse;
import com.lastcalleats.auth.dto.LoginRequest;
import com.lastcalleats.auth.dto.MerchantRegisterRequest;
import com.lastcalleats.auth.dto.UserRegisterRequest;

/**
 * This interface defines the main authentication operations provided by the auth module.
 * It serves as the contract between the controller layer and the service implementation.
 *
 * By defining these operations in an interface, the module becomes easier to maintain,
 * test, and extend in the future.
 */
public interface AuthService {

  /**
   * Registers a new normal user account and returns authentication information.
   *
   * The implementation is responsible for validating business rules, saving the user,
   * and generating a JWT token after successful registration.
   *
   * @param request the registration request for a normal user
   * @return authentication information for the newly registered user
   */
  AuthResponse registerUser(UserRegisterRequest request);

  /**
   * Registers a new merchant account and returns authentication information.
   *
   * The implementation creates the merchant account, saves it to the database,
   * and generates a JWT token for the merchant.
   *
   * @param request the registration request for a merchant
   * @return authentication information for the newly registered merchant
   */
  AuthResponse registerMerchant(MerchantRegisterRequest request);

  /**
   * Authenticates an existing account and returns authentication information.
   *
   * The implementation verifies the provided credentials and generates a JWT token
   * when the login request is successful.
   *
   * @param request the login request containing email and password
   * @param role the account role used to determine whether to log in as user or merchant
   * @return authentication information for the authenticated account
   */
  AuthResponse login(LoginRequest request, String role);
}