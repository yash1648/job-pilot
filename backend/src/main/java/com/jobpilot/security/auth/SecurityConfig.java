package com.jobpilot.security.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Security filter chain (doc 22 §1-3):
 * - JWT bearer auth for /api/v1/**; /auth/** entry points permitAll
 * - stateless sessions (JWT, doc 22 §1)
 * - CSRF enabled for Thymeleaf-form requests, exempt for API (doc 22 §3)
 * - session fixation protection on login (doc 22 §3)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        // API (JWT-bearer) requests are exempt from CSRF (doc 22 §3);
                        // non-API (Thymeleaf form) paths keep CSRF protection.
                        .ignoringRequestMatchers("/api/v1/**"))
                .sessionManagement(sm -> sm
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        .sessionFixation().changeSessionId())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login",
                                "/api/v1/auth/logout", "/api/v1/auth/refresh",
                                "/api/v1/auth/oauth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(eh -> eh
                        // unauthenticated API access → 401 (doc 05 §12), not 403
                        .authenticationEntryPoint((request, response, ex) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"error\":{\"code\":\"UNAUTHORIZED\",\"message\":\"authentication required\",\"details\":null}}");
                        }))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
