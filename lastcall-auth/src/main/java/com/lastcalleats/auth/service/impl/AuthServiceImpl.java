package com.lastcalleats.auth.service.impl;

import com.lastcalleats.auth.dto.AuthResponse;
import com.lastcalleats.auth.dto.LoginRequest;
import com.lastcalleats.auth.dto.MerchantRegisterRequest;
import com.lastcalleats.auth.dto.UserRegisterRequest;
import com.lastcalleats.auth.security.JwtUtil;
import com.lastcalleats.auth.service.AuthService;
import com.lastcalleats.common.exception.BusinessException;
import com.lastcalleats.common.exception.ErrorCode;
import com.lastcalleats.merchant.entity.MerchantDO;
import com.lastcalleats.merchant.repository.MerchantRepo;
import com.lastcalleats.user.entity.UserDO;
import com.lastcalleats.user.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * This class implements the authentication business logic for both users and merchants.
 * It handles registration, login, password encryption, duplicate account checking,
 * and token generation.
 *
 * The service acts as the central coordination layer between controllers, repositories,
 * and JWT utilities. It ensures that authentication-related rules are handled consistently.
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final UserRepo userRepo;
  private final MerchantRepo merchantRepo;
  private final JwtUtil jwtUtil;
  private final PasswordEncoder passwordEncoder;

  @Value("${app.jwt.expiration-ms:86400000}")
  private Long expirationMs;

  /**
   * Registers a new user account.
   *
   * This method first checks whether the email is already used. If the email is available,
   * it encrypts the password, saves the account, generates a JWT token, and returns
   * an authentication response.
   *
   * @param request the user registration request
   * @return authentication information for the registered user
   */
  @Override
  public AuthResponse registerUser(UserRegisterRequest request) {
    if (userRepo.existsByEmail(request.getEmail())) {
      throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
    }

    UserDO user = new UserDO();
    user.setEmail(request.getEmail());
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    user.setNickname(request.getNickname());

    user = userRepo.save(user);

    String token = jwtUtil.generateToken(user.getId(), "ROLE_USER");
    return buildResponse(token, user.getId(), user.getEmail(), "ROLE_USER");
  }

  /**
   * Registers a new merchant account.
   *
   * This method validates merchant uniqueness by email, encrypts the password,
   * stores merchant information in the database, and generates a JWT token.
   *
   * @param request the merchant registration request
   * @return authentication information for the registered merchant
   */
  @Override
  public AuthResponse registerMerchant(MerchantRegisterRequest request) {
    if (merchantRepo.existsByEmail(request.getEmail())) {
      throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
    }

    MerchantDO merchant = new MerchantDO();
    merchant.setEmail(request.getEmail());
    merchant.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    merchant.setName(request.getName());
    merchant.setAddress(request.getAddress());

    merchant = merchantRepo.save(merchant);

    String token = jwtUtil.generateToken(merchant.getId(), "ROLE_MERCHANT");
    return buildResponse(token, merchant.getId(), merchant.getEmail(), "ROLE_MERCHANT");
  }

  /**
   * Authenticates an existing user or merchant based on role.
   *
   * This method checks account existence, validates the password, generates a JWT token,
   * and returns authentication information. It supports both normal user login
   * and merchant login.
   *
   * @param request the login request containing email and password
   * @param role the role used to determine whether the account is a user or merchant
   * @return authentication information for the logged-in account
   */
  @Override
  public AuthResponse login(LoginRequest request, String role) {
    if ("USER".equalsIgnoreCase(role)) {
      UserDO user = userRepo.findByEmail(request.getEmail())
          .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

      if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
        throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
      }

      String token = jwtUtil.generateToken(user.getId(), "ROLE_USER");
      return buildResponse(token, user.getId(), user.getEmail(), "ROLE_USER");
    }

    if ("MERCHANT".equalsIgnoreCase(role)) {
      MerchantDO merchant = merchantRepo.findByEmail(request.getEmail())
          .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

      if (!passwordEncoder.matches(request.getPassword(), merchant.getPasswordHash())) {
        throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
      }

      String token = jwtUtil.generateToken(merchant.getId(), "ROLE_MERCHANT");
      return buildResponse(token, merchant.getId(), merchant.getEmail(), "ROLE_MERCHANT");
    }

    throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid role");
  }

  /**
   * Builds a standardized authentication response object.
   *
   * This helper method collects token information and account identity fields
   * into a single DTO that can be returned to the client.
   *
   * @param token the generated JWT token
   * @param userId the ID of the authenticated account
   * @param email the email of the authenticated account
   * @param role the role of the authenticated account
   * @return a populated authentication response
   */
  public AuthResponse buildResponse(String token, Long userId, String email, String role) {
    AuthResponse response = new AuthResponse();
    response.setAccessToken(token);
    response.setTokenType("Bearer");
    response.setExpiresIn(expirationMs);
    response.setUserId(userId);
    response.setEmail(email);
    response.setRole(role);
    return response;
  }
}