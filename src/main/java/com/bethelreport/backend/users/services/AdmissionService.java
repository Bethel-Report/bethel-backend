package com.bethelreport.backend.users.services;

import com.bethelreport.backend.invitations.entities.InvitationStatus;
import com.bethelreport.backend.invitations.repositories.InvitationRepository;
import com.bethelreport.backend.users.entities.User;
import com.bethelreport.backend.users.repositories.UserRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class AdmissionService {
  private final UserRepository userRepository;
  private final InvitationRepository invitationRepository;

  public AdmissionService(UserRepository userRepository,
      InvitationRepository invitationRepository) {
    this.userRepository = userRepository;
    this.invitationRepository = invitationRepository;
  }

  public boolean admittedUser(Jwt jwt) {
    String email = jwt.getClaimAsString("email");

    Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");

    if (email == null || email.isBlank()) {
      return false;
    }

    if (!Boolean.TRUE.equals(emailVerified)) {
      return Boolean.TRUE.equals(emailVerified);
    }

    return userRepository.findByEmailIgnoreCase(email).filter(User::isActive).isPresent()
        || invitationRepository.existsByEmailIgnoreCaseAndStatus(email, InvitationStatus.PENDING);
  }
}
