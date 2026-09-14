package com.lastcalleats.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lastcalleats.auth.dto.AuthResponse;
import com.lastcalleats.auth.dto.LoginRequest;
import com.lastcalleats.auth.dto.UserRegisterRequest;
import com.lastcalleats.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Mock
  private AuthService authService;

  @InjectMocks
  private AuthController authController;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
  }

  @Test
  void registerUser_success() throws Exception {
    UserRegisterRequest request = new UserRegisterRequest();
    request.setEmail("user@test.com");
    request.setPassword("123456");
    request.setNickname("Tom");

    AuthResponse response = new AuthResponse();
    response.setAccessToken("mock-token");
    response.setTokenType("Bearer");
    response.setExpiresIn(86400000L);
    response.setUserId(1L);
    response.setEmail("user@test.com");
    response.setRole("ROLE_USER");

    when(authService.registerUser(any(UserRegisterRequest.class))).thenReturn(response);

    mockMvc.perform(post("/api/auth/register/user")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("mock-token")))
        .andExpect(content().string(containsString("ROLE_USER")))
        .andExpect(content().string(containsString("user@test.com")));
  }

  @Test
  void loginUser_success() throws Exception {
    LoginRequest request = new LoginRequest();
    request.setEmail("user@test.com");
    request.setPassword("123456");

    AuthResponse response = new AuthResponse();
    response.setAccessToken("login-token");
    response.setTokenType("Bearer");
    response.setExpiresIn(86400000L);
    response.setUserId(1L);
    response.setEmail("user@test.com");
    response.setRole("ROLE_USER");

    when(authService.login(any(LoginRequest.class), eq("USER"))).thenReturn(response);

    mockMvc.perform(post("/api/auth/login/user")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("login-token")))
        .andExpect(content().string(containsString("ROLE_USER")))
        .andExpect(content().string(containsString("user@test.com")));
  }
}