package com.bethelreport.backend.users.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bethelreport.backend.users.entities.User;
import com.bethelreport.backend.users.repositories.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class AdmissionServiceTest {
  private final UserRepository userRepository = mock(UserRepository.class);
  private final AdmissionService admissionService = new AdmissionService(userRepository);

  @Test
  void admitsActiveUserWithVerifiedEmail() {
    String email = "user@example.com";
    User user = new User();
    user.setActive(true);

    when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));

    assertTrue(admissionService.admittedUser(jwt(email, true)));
  }

  @Test
  void rejectsInactiveUserWithVerifiedEmail() {
    String email = "user@example.com";
    User user = new User();
    user.setActive(false);

    when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));

    assertFalse(admissionService.admittedUser(jwt(email, true)));
  }

  private Jwt jwt(String email, boolean emailVerified) {
    return Jwt.withTokenValue("test-token").header("alg", "none").claim("email", email)
        .claim("email_verified", emailVerified).build();
  }
}
