package com.bethelreport.backend.users.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bethelreport.backend.bethel.entities.Bethel;
import com.bethelreport.backend.invitations.entities.Invitation;
import com.bethelreport.backend.invitations.entities.InvitationStatus;
import com.bethelreport.backend.invitations.repositories.InvitationRepository;
import com.bethelreport.backend.permissions.entities.Permission;
import com.bethelreport.backend.permissions.entities.Role;
import com.bethelreport.backend.permissions.repositories.PermissionRepository;
import com.bethelreport.backend.users.dtos.CompleteAdmissionRequest;
import com.bethelreport.backend.users.entities.User;
import com.bethelreport.backend.users.mappers.UserMapper;
import com.bethelreport.backend.users.repositories.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class AdmissionServiceTest {
  private final UserRepository userRepository = mock(UserRepository.class);
  private final InvitationRepository invitationRepository = mock(InvitationRepository.class);
  private final UserMapper userMapper = mock(UserMapper.class);
  private final PermissionRepository permissionRepository = mock(PermissionRepository.class);
  private final AdmissionService admissionService = new AdmissionService(userRepository,
      invitationRepository, mock(UserService.class), userMapper, permissionRepository);

  @Test
  void completesAdmissionWithPersistedUserBeforeCreatingPermission() {
    String email = "invited@example.com";
    Jwt token = Jwt.withTokenValue("test-token").header("alg", "none").subject("identity-id")
        .claim("email", email).claim("email_verified", true).build();
    Invitation invitation = new Invitation();
    invitation.setKeycloakUserId("identity-id");
    invitation.setStatus(InvitationStatus.PENDING);
    invitation.setRole(Role.MEMBRO_ATIVO);
    Bethel bethel = new Bethel();
    invitation.setBethel(bethel);
    CompleteAdmissionRequest request = new CompleteAdmissionRequest("Nome",
        LocalDate.of(2000, 1, 1), "Fortaleza", "Rua de teste", false, false, true, false);
    User transientUser = new User();
    User savedUser = new User();
    savedUser.setId(123L);
    when(invitationRepository.findByEmailForUpdate(email)).thenReturn(Optional.of(invitation));
    when(userMapper.complete(request)).thenReturn(transientUser);
    when(userRepository.save(transientUser)).thenReturn(savedUser);
    doAnswer(invocation -> {
      Permission permission = invocation.getArgument(0);
      verify(userRepository).save(transientUser);
      assertSame(savedUser, permission.getUser());
      assertEquals(Role.MEMBRO_ATIVO, permission.getRole());
      assertSame(bethel, permission.getBethel());
      assertTrue(permission.isActive());
      return permission;
    }).when(permissionRepository).save(any(Permission.class));

    admissionService.complete(token, request);

    assertEquals(email, transientUser.getEmail());
    assertEquals("identity-id", transientUser.getKeycloakUserId());
    verify(permissionRepository).save(any(Permission.class));
    assertEquals(InvitationStatus.ACCEPTED, invitation.getStatus());
    verify(invitationRepository).save(invitation);
    verify(userMapper).toResponse(savedUser);
  }

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

  @Test
  void admitsUserWithPendingInvitation() {
    String email = "invited@example.com";

    when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());
    when(invitationRepository.existsByEmailIgnoreCaseAndStatus(email, InvitationStatus.PENDING))
        .thenReturn(true);

    assertTrue(admissionService.admittedUser(jwt(email, true)));
  }

  @Test
  void rejectsUserWithoutInvitationOrUserRecord() {
    String email = "unknown@example.com";

    when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());
    when(invitationRepository.existsByEmailIgnoreCaseAndStatus(email, InvitationStatus.PENDING))
        .thenReturn(false);

    assertFalse(admissionService.admittedUser(jwt(email, true)));
  }

  @Test
  void rejectsUserWithUnverifiedEmail() {
    assertFalse(admissionService.admittedUser(jwt("user@example.com", false)));
  }

  @Test
  void rejectsJwtWithoutEmail() {
    Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").claim("email_verified", true)
        .build();

    assertFalse(admissionService.admittedUser(jwt));
  }

  @Test
  void rejectsJwtWithBlankEmail() {
    assertFalse(admissionService.admittedUser(jwt("   ", true)));
  }

  private Jwt jwt(String email, boolean emailVerified) {
    return Jwt.withTokenValue("test-token").header("alg", "none").claim("email", email)
        .claim("email_verified", emailVerified).build();
  }
}
