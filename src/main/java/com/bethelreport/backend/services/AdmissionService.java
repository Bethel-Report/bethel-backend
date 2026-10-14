package com.bethelreport.backend.services;

import com.bethelreport.backend.entities.User;
import com.bethelreport.backend.repositories.UserRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class AdmissionService {
  private final UserRepository userRepository;

  public AdmissionService(UserRepository usuarioRepository) {
    this.userRepository = usuarioRepository;
  }

  public boolean admittedUser(Jwt jwt) {
    String email = jwt.getClaimAsString("email");

    Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");

    if (email == null || email.isBlank()) {
      return false;
    }

    if (!Boolean.TRUE.equals(emailVerified)) {
      return false;
    }

    return userRepository.findByEmailIgnoreCase(email).filter(User::isActive).isPresent();
  }
}
