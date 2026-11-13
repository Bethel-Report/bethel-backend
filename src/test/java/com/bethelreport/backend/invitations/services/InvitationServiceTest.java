package com.bethelreport.backend.invitations.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.bethelreport.backend.bethel.entities.Bethel;
import com.bethelreport.backend.bethel.repositories.BethelRepository;
import com.bethelreport.backend.exceptions.DuplicateEmailException;
import com.bethelreport.backend.exceptions.InvitationNotFoundException;
import com.bethelreport.backend.invitations.dtos.CreateInvitationRequest;
import com.bethelreport.backend.invitations.dtos.InvitationResponse;
import com.bethelreport.backend.invitations.entities.Invitation;
import com.bethelreport.backend.invitations.entities.InvitationStatus;
import com.bethelreport.backend.invitations.mappers.InvitationMapper;
import com.bethelreport.backend.invitations.mappers.InvitationMapperImpl;
import com.bethelreport.backend.invitations.repositories.InvitationRepository;
import com.bethelreport.backend.keycloak.services.KeycloakUserService;
import com.bethelreport.backend.permissions.entities.Role;
import com.bethelreport.backend.permissions.services.AuthorizationService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class InvitationServiceTest {
  private final InvitationRepository invitationRepository = mock(InvitationRepository.class);
  private final InvitationMapper invitationMapper = new InvitationMapperImpl();
  private final AuthorizationService authorizationService = mock(AuthorizationService.class);
  private final BethelRepository bethelRepository = mock(BethelRepository.class);
  private final KeycloakUserService keycloakUserService = mock(KeycloakUserService.class);
  private final InvitationService invitationService = new InvitationService(invitationRepository,
      invitationMapper, authorizationService, bethelRepository, keycloakUserService);
  private final Authentication authentication = authentication("council@example.com");
  private int invitationRepositorySaveCount;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.getContext().setAuthentication(authentication);
    when(authorizationService.isGrandeConselho(authentication)).thenReturn(false);
    when(authorizationService.hasRole(authentication, Role.CONSELHO_DE_BETHEL)).thenReturn(true);
    when(authorizationService.getAuthorizedBethelId(authentication)).thenReturn(10L);
    when(authorizationService.canManageBethel(eq(10L), eq(authentication))).thenReturn(true);
  }

  @Test
  void createsInvitationSuccessfully() {
    CreateInvitationRequest request = new CreateInvitationRequest("new@example.com", 10L, Role.MEMBRO_ATIVO);

    when(bethelRepository.existsById(10L)).thenReturn(true);
    when(bethelRepository.getReferenceById(10L)).thenReturn(buildBethel(10L));
    when(invitationRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
    when(invitationRepository.save(any(Invitation.class))).thenAnswer(invocation -> {
      Invitation saved = invocation.getArgument(0);
      saved.setId(1L);
      return saved;
    });

    when(keycloakUserService.provisionUser("new@example.com", null, null)).thenReturn("kc-123");

    InvitationResponse response = invitationService.createInvitation(request, authentication);

    assertEquals(1L, response.id());
    assertEquals("new@example.com", response.email());
    assertEquals(InvitationStatus.PENDING, response.status());
    assertEquals(10L, response.bethelId());

    verify(invitationRepository, times(2)).save(any(Invitation.class));
    verify(keycloakUserService).provisionUser("new@example.com", null, null);
    verify(keycloakUserService).sendInvitationActionsEmail("kc-123");
  }

  @Test
  void deletesProvisionedIdentityWhenEmailPublicationFails() {
    CreateInvitationRequest request = new CreateInvitationRequest("new@example.com", 10L, Role.MEMBRO_ATIVO);
    when(bethelRepository.existsById(10L)).thenReturn(true);
    when(bethelRepository.getReferenceById(10L)).thenReturn(buildBethel(10L));
    when(invitationRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
    when(invitationRepository.save(any(Invitation.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(keycloakUserService.provisionUser("new@example.com", null, null)).thenReturn("kc-456");
    doThrow(new IllegalStateException("Keycloak unavailable")).when(keycloakUserService)
        .sendInvitationActionsEmail("kc-456");

    TransactionSynchronizationManager.initSynchronization();
    try {
      assertThrows(IllegalStateException.class,
          () -> invitationService.createInvitation(request, authentication));
      completeRollback();
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
    verify(keycloakUserService).deleteUser("kc-456");
  }

  @Test
  void deletesProvisionedIdentityWhenInvitationPersistenceFails() {
    CreateInvitationRequest request = new CreateInvitationRequest("new@example.com", 10L, Role.MEMBRO_ATIVO);
    when(bethelRepository.existsById(10L)).thenReturn(true);
    when(bethelRepository.getReferenceById(10L)).thenReturn(buildBethel(10L));
    when(invitationRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
    when(invitationRepository.save(any(Invitation.class))).thenAnswer(invocation -> {
      if (invitationRepositorySaveCount++ == 0) {
        return invocation.getArgument(0);
      }
      throw new IllegalStateException("database unavailable");
    });
    when(keycloakUserService.provisionUser("new@example.com", null, null)).thenReturn("kc-789");

    TransactionSynchronizationManager.initSynchronization();
    try {
      assertThrows(IllegalStateException.class,
          () -> invitationService.createInvitation(request, authentication));
      completeRollback();
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
    verify(keycloakUserService).deleteUser("kc-789");
    verify(keycloakUserService, never()).sendInvitationActionsEmail(any());
  }

  @Test
  void createInvitationThrowsOnDuplicateEmail() {
    CreateInvitationRequest request = new CreateInvitationRequest("dup@example.com", 10L, Role.MEMBRO_ATIVO);

    when(bethelRepository.existsById(10L)).thenReturn(true);
    when(invitationRepository.existsByEmailIgnoreCase("dup@example.com")).thenReturn(true);

    assertThrows(DuplicateEmailException.class,
        () -> invitationService.createInvitation(request, authentication));
    verify(invitationRepository, never()).save(any());
    verify(keycloakUserService, never()).sendInvitationActionsEmail(any());
  }

  @Test
  void createInvitationThrowsWhenBethelNotFound() {
    CreateInvitationRequest request = new CreateInvitationRequest("new@example.com", 99L, Role.MEMBRO_ATIVO);

    when(bethelRepository.existsById(99L)).thenReturn(false);

    assertThrows(ResponseStatusException.class,
        () -> invitationService.createInvitation(request, authentication));
    verify(invitationRepository, never()).save(any());
    verify(keycloakUserService, never()).sendInvitationActionsEmail(any());
  }

  @Test
  void createInvitationThrowsWhenUnauthorizedForBethel() {
    CreateInvitationRequest request = new CreateInvitationRequest("new@example.com", 20L, Role.MEMBRO_ATIVO);

    when(bethelRepository.existsById(20L)).thenReturn(true);
    when(authorizationService.canManageBethel(eq(20L), eq(authentication))).thenReturn(false);

    assertThrows(AccessDeniedException.class,
        () -> invitationService.createInvitation(request, authentication));
    verify(invitationRepository, never()).save(any());
    verify(keycloakUserService, never()).sendInvitationActionsEmail(any());
  }

  @Test
  void councilListsOnlyInvitationsFromAuthorizedBethel() {
    Invitation invitation = buildInvitation(1L, "a@example.com", InvitationStatus.PENDING, 10L, Role.MEMBRO_ATIVO);

    when(invitationRepository.findByBethelId(10L)).thenReturn(List.of(invitation));

    List<InvitationResponse> result = invitationService.listInvitations();

    assertEquals(1, result.size());
    assertEquals("a@example.com", result.getFirst().email());
    verify(invitationRepository).findByBethelId(10L);
    verify(invitationRepository, never()).findAll();
  }

  @Test
  void grandCouncilListsAllInvitations() {
    when(authorizationService.isGrandeConselho(authentication)).thenReturn(true);
    Invitation invitation = buildInvitation(1L, "a@example.com", InvitationStatus.PENDING, 10L, Role.MEMBRO_ATIVO);

    when(invitationRepository.findAll()).thenReturn(List.of(invitation));

    List<InvitationResponse> result = invitationService.listInvitations();

    assertEquals(1, result.size());
    verify(invitationRepository).findAll();
    verify(invitationRepository, never()).findByBethelId(any());
  }

  @Test
  void getsInvitationById() {
    Invitation invitation = buildInvitation(1L, "a@example.com", InvitationStatus.PENDING, 10L, Role.MEMBRO_ATIVO);

    when(invitationRepository.findById(1L)).thenReturn(Optional.of(invitation));

    InvitationResponse response = invitationService.getInvitationById(1L, authentication);

    assertEquals(1L, response.id());
    assertEquals("a@example.com", response.email());
    assertEquals(10L, response.bethelId());
  }

  @Test
  void getInvitationByIdThrowsWhenNotFound() {
    when(invitationRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(InvitationNotFoundException.class,
        () -> invitationService.getInvitationById(99L, authentication));
  }

  @Test
  void getInvitationByIdThrowsWhenUnauthorizedForBethel() {
    Invitation invitation = buildInvitation(1L, "a@example.com", InvitationStatus.PENDING, 20L, Role.MEMBRO_ATIVO);

    when(invitationRepository.findById(1L)).thenReturn(Optional.of(invitation));
    when(authorizationService.canManageBethel(eq(20L), eq(authentication))).thenReturn(false);

    assertThrows(AccessDeniedException.class,
        () -> invitationService.getInvitationById(1L, authentication));
  }

  @Test
  void revokesInvitation() {
    Invitation invitation = buildInvitation(1L, "a@example.com", InvitationStatus.PENDING, 10L, Role.MEMBRO_ATIVO);

    when(invitationRepository.findById(1L)).thenReturn(Optional.of(invitation));

    invitationService.revokeInvitation(1L, authentication);

    verify(invitationRepository).delete(invitation);
  }

  @Test
  void revokeInvitationThrowsWhenNotFound() {
    when(invitationRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(InvitationNotFoundException.class,
        () -> invitationService.revokeInvitation(99L, authentication));
    verify(invitationRepository, never()).delete(any());
  }

  @Test
  void revokeInvitationThrowsWhenUnauthorizedForBethel() {
    Invitation invitation = buildInvitation(1L, "a@example.com", InvitationStatus.PENDING, 20L, Role.MEMBRO_ATIVO);

    when(invitationRepository.findById(1L)).thenReturn(Optional.of(invitation));
    when(authorizationService.canManageBethel(eq(20L), eq(authentication))).thenReturn(false);

    assertThrows(AccessDeniedException.class,
        () -> invitationService.revokeInvitation(1L, authentication));
    verify(invitationRepository, never()).delete(any());
  }

  private void completeRollback() {
    TransactionSynchronizationManager.getSynchronizations()
        .forEach(synchronization -> synchronization
            .afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
  }

  private Invitation buildInvitation(Long id, String email, InvitationStatus status,
      Long bethelId, Role role) {
    Invitation invitation = new Invitation();
    invitation.setId(id);
    invitation.setEmail(email);
    invitation.setStatus(status);
    invitation.setRole(role);
    invitation.setBethel(buildBethel(bethelId));
    invitation.setCreatedAt(LocalDateTime.of(2026, 10, 31, 10, 0));
    return invitation;
  }

  private Bethel buildBethel(Long id) {
    Bethel bethel = new Bethel();
    bethel.setId(id);
    return bethel;
  }

  private Authentication authentication(String email) {
    Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("email", email).build();
    return new JwtAuthenticationToken(jwt);
  }
}
