package com.bethelreport.backend.users.services;

import com.bethelreport.backend.invitations.entities.Invitation;
import com.bethelreport.backend.invitations.entities.InvitationStatus;
import com.bethelreport.backend.invitations.repositories.InvitationRepository;
import com.bethelreport.backend.permissions.entities.Permission;
import com.bethelreport.backend.permissions.repositories.PermissionRepository;
import com.bethelreport.backend.users.dtos.CompleteAdmissionRequest;
import com.bethelreport.backend.users.dtos.UserResponse;
import com.bethelreport.backend.users.entities.User;
import com.bethelreport.backend.users.mappers.UserMapper;
import com.bethelreport.backend.users.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdmissionService {
  private final UserRepository userRepository;
  private final InvitationRepository invitationRepository;
  private final UserMapper userMapper;
  private final PermissionRepository permissionRepository;

  public AdmissionService(UserRepository userRepository, InvitationRepository invitationRepository,
      UserService userService, UserMapper userMapper, PermissionRepository permissionRepository) {
    this.userRepository = userRepository;
    this.invitationRepository = invitationRepository;
    this.userMapper = userMapper;
    this.permissionRepository = permissionRepository;
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

  @Transactional
  public UserResponse complete(Jwt jwt, CompleteAdmissionRequest request) {

    String email = jwt.getClaimAsString("email");
    String keycloakUserId = jwt.getSubject();

    Boolean emailVerified = jwt.getClaim("email_verified");

    validateClaims(email, emailVerified);

    Invitation invitation = invitationRepository.findByEmailForUpdate(email)
        .orElseThrow(() -> new AccessDeniedException("Não existe convite para este email."));

    validateInvitation(email, keycloakUserId, invitation);

    User user = userMapper.complete(request);

    user.setEmail(email);
    user.setKeycloakUserId(keycloakUserId);

    user = userRepository.save(user);

    Permission permission = new Permission();
    permission.setUser(user);
    permission.setRole(invitation.getRole());
    permission.setBethel(invitation.getBethel());
    permission.setActive(true);

    permissionRepository.save(permission);

    invitation.setStatus(InvitationStatus.ACCEPTED);

    invitationRepository.save(invitation);

    return userMapper.toResponse(user);
  }

  private void validateClaims(String email, Boolean emailVerified) {

    if (email == null || email.isBlank()) {
      throw new AccessDeniedException("O token não possui email.");
    }

    if (!Boolean.TRUE.equals(emailVerified)) {
      throw new AccessDeniedException("O email não foi verificado.");
    }

  }

  private void validateInvitation(String email, String keycloakUserId, Invitation invitation) {

    if (!invitation.getKeycloakUserId().equals(keycloakUserId)) {
      throw new AccessDeniedException("Este convite não pertence ao usuário autenticado.");
    }

    if (invitation.getStatus() != InvitationStatus.PENDING) {
      throw new IllegalStateException("Este convite não está disponível para admissão.");
    }

    if (userRepository.existsByEmailIgnoreCase(email)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "O usuário já possui cadastro.");
    }
  }
}
