package com.lastcalleats.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * This filter processes JWT tokens in incoming HTTP requests.
 * It is executed once per request before the request reaches protected controllers.
 *
 * The filter reads the Authorization header, validates the token, and extracts
 * user identity information. If the token is valid, it stores authentication data
 * in the Spring Security context so that the request can be treated as authenticated.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtUtil jwtUtil;

  /**
   * Filters each incoming request and attempts to authenticate the user based on a JWT token.
   *
   * If the Authorization header contains a valid Bearer token, this method extracts the
   * user ID and role from the token and stores them in the SecurityContext. If the token
   * is missing or invalid, the filter simply continues the chain without authentication.
   *
   * @param request the incoming HTTP request
   * @param response the outgoing HTTP response
   * @param filterChain the remaining filter chain
   * @throws ServletException if a servlet-related error occurs
   * @throws IOException if an input/output error occurs
   */
  @Override
  protected void doFilterInternal(HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String authHeader = request.getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = authHeader.substring(7);

    if (jwtUtil.validateToken(token)) {
      Long userId = jwtUtil.extractUserId(token);
      String role = jwtUtil.extractRole(token);

      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(
              userId,
              null,
              Collections.singletonList(new SimpleGrantedAuthority(role))
          );

      SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    filterChain.doFilter(request, response);
  }
}