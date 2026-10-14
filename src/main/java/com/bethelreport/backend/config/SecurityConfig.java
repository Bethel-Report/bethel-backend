package com.bethelreport.backend.config;

import com.bethelreport.backend.services.AdmissionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
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
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

    return http.build();
  }
}
