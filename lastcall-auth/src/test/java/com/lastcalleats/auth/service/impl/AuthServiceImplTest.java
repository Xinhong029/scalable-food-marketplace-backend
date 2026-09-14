package com.lastcalleats.auth.service.impl;

import com.lastcalleats.auth.dto.AuthResponse;
import com.lastcalleats.auth.dto.LoginRequest;
import com.lastcalleats.auth.dto.MerchantRegisterRequest;
import com.lastcalleats.auth.dto.UserRegisterRequest;
import com.lastcalleats.auth.security.JwtUtil;
import com.lastcalleats.common.exception.BusinessException;
import com.lastcalleats.common.exception.ErrorCode;
import com.lastcalleats.merchant.entity.MerchantDO;
import com.lastcalleats.merchant.repository.MerchantRepo;
import com.lastcalleats.user.entity.UserDO;
import com.lastcalleats.user.repository.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

  @Mock
  private UserRepo userRepo;

  @Mock
  private MerchantRepo merchantRepo;

  @Mock
  private JwtUtil jwtUtil;

  @Mock
  private PasswordEncoder passwordEncoder;

  @InjectMocks
  private AuthServiceImpl authService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(authService, "expirationMs", 86400000L);
  }

  @Test
  void registerUser_success() {
    UserRegisterRequest request = new UserRegisterRequest();
    request.setEmail("user@test.com");
    request.setPassword("123456");
    request.setNickname("Tom");

    when(userRepo.existsByEmail("user@test.com")).thenReturn(false);
    when(passwordEncoder.encode("123456")).thenReturn("encodedPwd");

    UserDO savedUser = new UserDO();
    savedUser.setId(1L);
    savedUser.setEmail("user@test.com");
    savedUser.setNickname("Tom");
    savedUser.setPasswordHash("encodedPwd");

    when(userRepo.save(any(UserDO.class))).thenReturn(savedUser);
    when(jwtUtil.generateToken(1L, "ROLE_USER")).thenReturn("mock-user-token");

    AuthResponse response = authService.registerUser(request);

    assertNotNull(response);
    assertEquals("mock-user-token", response.getAccessToken());
    assertEquals("ROLE_USER", response.getRole());
    assertEquals(1L, response.getUserId());
    assertEquals("user@test.com", response.getEmail());

    verify(userRepo).existsByEmail("user@test.com");
    verify(passwordEncoder).encode("123456");
    verify(userRepo).save(any(UserDO.class));
    verify(jwtUtil).generateToken(1L, "ROLE_USER");
  }

  @Test
  void registerUser_duplicateEmail_shouldThrowException() {
    UserRegisterRequest request = new UserRegisterRequest();
    request.setEmail("user@test.com");
    request.setPassword("123456");
    request.setNickname("Tom");

    when(userRepo.existsByEmail("user@test.com")).thenReturn(true);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> authService.registerUser(request));

    assertEquals(ErrorCode.EMAIL_ALREADY_EXISTS, ex.getErrorCode());
    verify(userRepo).existsByEmail("user@test.com");
    verify(userRepo, never()).save(any());
  }

  @Test
  void registerMerchant_success() {
    MerchantRegisterRequest request = new MerchantRegisterRequest();
    request.setEmail("merchant@test.com");
    request.setPassword("123456");
    request.setName("Test Shop");
    request.setAddress("Boston");

    when(merchantRepo.existsByEmail("merchant@test.com")).thenReturn(false);
    when(passwordEncoder.encode("123456")).thenReturn("encodedPwd");

    MerchantDO savedMerchant = new MerchantDO();
    savedMerchant.setId(2L);
    savedMerchant.setEmail("merchant@test.com");
    savedMerchant.setName("Test Shop");
    savedMerchant.setAddress("Boston");
    savedMerchant.setPasswordHash("encodedPwd");

    when(merchantRepo.save(any(MerchantDO.class))).thenReturn(savedMerchant);
    when(jwtUtil.generateToken(2L, "ROLE_MERCHANT")).thenReturn("mock-merchant-token");

    AuthResponse response = authService.registerMerchant(request);

    assertNotNull(response);
    assertEquals("mock-merchant-token", response.getAccessToken());
    assertEquals("ROLE_MERCHANT", response.getRole());
    assertEquals(2L, response.getUserId());
    assertEquals("merchant@test.com", response.getEmail());
  }

  @Test
  void login_user_success() {
    LoginRequest request = new LoginRequest();
    request.setEmail("user@test.com");
    request.setPassword("123456");

    UserDO user = new UserDO();
    user.setId(1L);
    user.setEmail("user@test.com");
    user.setPasswordHash("encodedPwd");

    when(userRepo.findByEmail("user@test.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("123456", "encodedPwd")).thenReturn(true);
    when(jwtUtil.generateToken(1L, "ROLE_USER")).thenReturn("login-user-token");

    AuthResponse response = authService.login(request, "USER");

    assertNotNull(response);
    assertEquals("login-user-token", response.getAccessToken());
    assertEquals("ROLE_USER", response.getRole());
    assertEquals(1L, response.getUserId());
  }

  @Test
  void login_user_wrongPassword_shouldThrowException() {
    LoginRequest request = new LoginRequest();
    request.setEmail("user@test.com");
    request.setPassword("wrongPwd");

    UserDO user = new UserDO();
    user.setId(1L);
    user.setEmail("user@test.com");
    user.setPasswordHash("encodedPwd");

    when(userRepo.findByEmail("user@test.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrongPwd", "encodedPwd")).thenReturn(false);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> authService.login(request, "USER"));

    assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
    verify(jwtUtil, never()).generateToken(anyLong(), anyString());
  }

  @Test
  void login_user_notFound_shouldThrowException() {
    LoginRequest request = new LoginRequest();
    request.setEmail("notfound@test.com");
    request.setPassword("123456");

    when(userRepo.findByEmail("notfound@test.com")).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> authService.login(request, "USER"));

    assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
  }

  @Test
  void login_merchant_success() {
    LoginRequest request = new LoginRequest();
    request.setEmail("merchant@test.com");
    request.setPassword("123456");

    MerchantDO merchant = new MerchantDO();
    merchant.setId(2L);
    merchant.setEmail("merchant@test.com");
    merchant.setPasswordHash("encodedPwd");

    when(merchantRepo.findByEmail("merchant@test.com")).thenReturn(Optional.of(merchant));
    when(passwordEncoder.matches("123456", "encodedPwd")).thenReturn(true);
    when(jwtUtil.generateToken(2L, "ROLE_MERCHANT")).thenReturn("login-merchant-token");

    AuthResponse response = authService.login(request, "MERCHANT");

    assertNotNull(response);
    assertEquals("login-merchant-token", response.getAccessToken());
    assertEquals("ROLE_MERCHANT", response.getRole());
    assertEquals(2L, response.getUserId());
  }
}