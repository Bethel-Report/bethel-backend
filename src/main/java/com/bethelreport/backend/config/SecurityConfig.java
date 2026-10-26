package com.bethelreport.backend.config;

import com.bethelreport.backend.users.services.AdmissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  private final AdmissionService admissionService;

  public SecurityConfig(AdmissionService admissionService) {
    this.admissionService = admissionService;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) {

    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/health").permitAll()
            .anyRequest().access((authentication, context) -> {
              Authentication current = authentication.get();

              if (!(current instanceof JwtAuthenticationToken jwtAuth)) {
                return new AuthorizationDecision(false);
              }

              boolean admitted = admissionService.admittedUser(jwtAuth.getToken());

              return new AuthorizationDecision(admitted);
            }))
        .exceptionHandling(exception -> exception.authenticationEntryPoint(this::writeUnauthorized)
            .accessDeniedHandler(this::writeForbidden))
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults())
            .authenticationEntryPoint(this::writeUnauthorized)
            .accessDeniedHandler(this::writeForbidden));

    return http.build();
  }

  @SuppressWarnings("PMD.UnusedFormalParameter")
  private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response,
      org.springframework.security.core.AuthenticationException exception) throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json");
    response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\","
        + "\"message\":\"Autenticação necessária.\"}");
  }

  @SuppressWarnings("PMD.UnusedFormalParameter")
  private void writeForbidden(HttpServletRequest request, HttpServletResponse response,
      org.springframework.security.access.AccessDeniedException exception) throws IOException {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType("application/json");
    response.getWriter().write(
        "{\"status\":403,\"error\":\"Forbidden\"," + "\"message\":\"Acesso não permitido.\"}");
  }
}
