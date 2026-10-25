package com.bethelreport.backend.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class AuthorizationServiceTestConfiguration {

  @Bean(name = "authorizationService")
  AuthorizationService authorizationService() {
    return new AuthorizationService(null) {
      @Override
      public boolean isGrandeConselho(
          org.springframework.security.core.Authentication authentication) {
        return true;
      }

      @Override
      public boolean canAccessBethel(Long bethelId,
          org.springframework.security.core.Authentication authentication) {
        return true;
      }

      @Override
      public boolean canManageBethel(Long bethelId,
          org.springframework.security.core.Authentication authentication) {
        return true;
      }

      @Override
      public boolean canManageUsers(
          org.springframework.security.core.Authentication authentication) {
        return true;
      }
    };
  }
}
