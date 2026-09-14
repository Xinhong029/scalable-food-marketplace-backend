package com.lastcalleats.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

  @Mock
  private JwtUtil jwtUtil;

  @Mock
  private FilterChain filterChain;

  @Mock
  private HttpServletResponse response;

  private JwtAuthFilter jwtAuthFilter;

  @BeforeEach
  void setUp() {
    jwtAuthFilter = new JwtAuthFilter(jwtUtil);
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void doFilterInternal_noAuthorizationHeader_shouldContinueFilterChain()
      throws ServletException, IOException {

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/test");

    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain, times(1)).doFilter(request, response);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verifyNoInteractions(jwtUtil);
  }

  @Test
  void doFilterInternal_invalidAuthorizationHeader_shouldContinueFilterChain()
      throws ServletException, IOException {

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "InvalidTokenFormat");

    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain, times(1)).doFilter(request, response);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verifyNoInteractions(jwtUtil);
  }

  @Test
  void doFilterInternal_validToken_shouldSetAuthentication()
      throws ServletException, IOException {

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer valid-token");

    when(jwtUtil.validateToken("valid-token")).thenReturn(true);
    when(jwtUtil.extractUserId("valid-token")).thenReturn(1L);
    when(jwtUtil.extractRole("valid-token")).thenReturn("ROLE_USER");

    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    assertNotNull(authentication);
    assertEquals(1L, authentication.getPrincipal());
    assertEquals(1, authentication.getAuthorities().size());
    assertEquals("ROLE_USER", authentication.getAuthorities().iterator().next().getAuthority());

    verify(jwtUtil).validateToken("valid-token");
    verify(jwtUtil).extractUserId("valid-token");
    verify(jwtUtil).extractRole("valid-token");
    verify(filterChain, times(1)).doFilter(request, response);
  }

  @Test
  void doFilterInternal_invalidToken_shouldNotSetAuthentication()
      throws ServletException, IOException {

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer invalid-token");

    when(jwtUtil.validateToken("invalid-token")).thenReturn(false);

    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());

    verify(jwtUtil).validateToken("invalid-token");
    verify(jwtUtil, never()).extractUserId(anyString());
    verify(jwtUtil, never()).extractRole(anyString());
    verify(filterChain, times(1)).doFilter(request, response);
  }
}