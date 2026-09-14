package com.lastcalleats.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

  private JwtUtil jwtUtil;

  @BeforeEach
  void setUp() {
    jwtUtil = new JwtUtil();
    ReflectionTestUtils.setField(jwtUtil, "secret", "thisIsASecretKeyForJwtTokenGeneration123456");
    ReflectionTestUtils.setField(jwtUtil, "expirationMs", 86400000L);
    jwtUtil.init();
  }

  @Test
  void generateAndParseToken_success() {
    String token = jwtUtil.generateToken(1L, "ROLE_USER");

    assertNotNull(token);
    assertTrue(jwtUtil.validateToken(token));
    assertEquals(1L, jwtUtil.extractUserId(token));
    assertEquals("ROLE_USER", jwtUtil.extractRole(token));
  }

  @Test
  void validateToken_invalidToken_shouldReturnFalse() {
    String invalidToken = "abc.def.ghi";
    assertFalse(jwtUtil.validateToken(invalidToken));
  }
}