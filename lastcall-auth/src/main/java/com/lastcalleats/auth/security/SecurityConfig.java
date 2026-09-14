package com.lastcalleats.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * This class defines the Spring Security configuration for the application.
 * It specifies which endpoints are public, which endpoints require authentication,
 * and how JWT-based security should be applied.
 *
 * The configuration uses stateless session management because the system relies on JWT
 * instead of server-side sessions to identify authenticated users.
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthFilter jwtAuthFilter;

  /**
   * Configures the main security filter chain for HTTP requests.
   *
   * This method disables CSRF for stateless APIs, enables JWT-based authentication,
   * allows public access to authentication endpoints, and protects other routes
   * based on user roles or authentication status.
   *
   * @param http the HttpSecurity builder used to configure web security
   * @return the configured SecurityFilterChain
   * @throws Exception if the security configuration fails
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/api/auth/login",
                "/api/auth/login/**",
                "/api/auth/register/**",
                "/api/products/browse/**",
                "/api/payment/webhook",
                "/actuator/health/**",
                "/actuator/metrics/**",
                "/actuator/prometheus",
                "/swagger-ui/**",
                "/api-docs/**",
                "/v3/api-docs/**"
            ).permitAll()
            .requestMatchers("/api/merchant/**").hasAuthority("ROLE_MERCHANT")
            .requestMatchers("/api/users/**", "/api/orders/**", "/api/payment/create")
            .hasAuthority("ROLE_USER")
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  /**
   * Creates the password encoder used by the authentication module.
   *
   * BCrypt is used here because it is a widely accepted password hashing algorithm
   * and provides a secure way to store user passwords.
   *
   * @return a BCrypt-based password encoder
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
