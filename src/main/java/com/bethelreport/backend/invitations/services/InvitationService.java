package com.bethelreport.backend.invitations.services;

import com.bethelreport.backend.bethel.entities.Bethel;
import com.bethelreport.backend.bethel.repositories.BethelRepository;
import com.bethelreport.backend.exceptions.DuplicateEmailException;
import com.bethelreport.backend.exceptions.InvitationNotFoundException;
import com.bethelreport.backend.invitations.dtos.CreateInvitationRequest;
import com.bethelreport.backend.invitations.dtos.InvitationResponse;
import com.bethelreport.backend.invitations.entities.Invitation;
import com.bethelreport.backend.invitations.entities.InvitationStatus;
import com.bethelreport.backend.invitations.mappers.InvitationMapper;
import com.bethelreport.backend.invitations.repositories.InvitationRepository;
import com.bethelreport.backend.keycloak.services.KeycloakUserService;
import com.bethelreport.backend.message.dtos.EmailMessage;
import com.bethelreport.backend.message.publishers.EmailPublisher;
import com.bethelreport.backend.permissions.entities.Role;
import com.bethelreport.backend.permissions.services.AuthorizationService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InvitationService {
  private final InvitationRepository invitationRepository;
  private final InvitationMapper invitationMapper;
  private final AuthorizationService authorizationService;
  private final BethelRepository bethelRepository;
  private final EmailPublisher emailPublisher;
  private final KeycloakUserService keycloakUserService;

  public InvitationService(InvitationRepository invitationRepository,
      InvitationMapper invitationMapper, AuthorizationService authorizationService,
      BethelRepository bethelRepository, EmailPublisher emailPublisher,
      KeycloakUserService keycloakUserService) {
    this.invitationRepository = invitationRepository;
    this.invitationMapper = invitationMapper;
    this.authorizationService = authorizationService;
    this.bethelRepository = bethelRepository;
    this.emailPublisher = emailPublisher;
    this.keycloakUserService = keycloakUserService;
  }

  @Transactional
  public InvitationResponse createInvitation(CreateInvitationRequest request,
      Authentication authentication) {
    validateInvitationRequest(request, authentication);

    Bethel bethel = bethelRepository.getReferenceById(request.bethelId());
    Invitation invitation = invitationMapper.toEntity(request);
    invitation.setStatus(InvitationStatus.PENDING);
    invitation.setCreatedAt(LocalDateTime.now());
    invitation.setBethel(bethel);

    Invitation saved = invitationRepository.save(invitation);
    String keycloakUserId = keycloakUserService.provisionUser(saved.getEmail(), null, null);
    registerKeycloakCompensation(keycloakUserId);
    saved.setKeycloakUserId(keycloakUserId);
    saved = invitationRepository.save(saved);
    publishInvitationEmail(saved);

    return invitationMapper.toResponse(saved);
  }

  private void validateInvitationRequest(CreateInvitationRequest request,
      Authentication authentication) {
    if (!bethelRepository.existsById(request.bethelId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bethel não encontrado.");
    }
    if (!authorizationService.canManageBethel(request.bethelId(), authentication)) {
      throw new AccessDeniedException("Você não pode convidar para este Bethel.");
    }
    if (invitationRepository.existsByEmailIgnoreCase(request.email())) {
      throw new DuplicateEmailException(request.email());
    }
  }

  private void publishInvitationEmail(Invitation invitation) {
    emailPublisher
        .publish(new EmailMessage(invitation.getEmail(), "Convite para acessar o Bethel Report",
            "invitation", Map.of("invitationLink", "link para aceitar o convite")));
  }

  private void registerKeycloakCompensation(String keycloakUserId) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCompletion(int status) {
        if (status == STATUS_ROLLED_BACK) {
          keycloakUserService.deleteUser(keycloakUserId);
        }
      }
    });
  }

  public List<InvitationResponse> listInvitations() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authorizationService.isGrandeConselho(authentication)) {
      return invitationRepository.findAll().stream().map(invitationMapper::toResponse).toList();
    }

    if (authorizationService.hasRole(authentication, Role.CONSELHO_DE_BETHEL)) {
      Long bethelId = authorizationService.getAuthorizedBethelId(authentication);
      return invitationRepository.findByBethelId(bethelId).stream()
          .map(invitationMapper::toResponse).toList();
    }

    throw new AccessDeniedException("Você não possui permissão para listar convites.");
  }

  public InvitationResponse getInvitationById(Long id, Authentication authentication) {
    return invitationMapper.toResponse(findAuthorizedInvitation(id, authentication));
  }

  public void revokeInvitation(Long id, Authentication authentication) {
    Invitation invitation = findAuthorizedInvitation(id, authentication);
    invitationRepository.delete(invitation);
  }

  private Invitation findAuthorizedInvitation(Long id, Authentication authentication) {
    Invitation invitation = invitationRepository.findById(id)
        .orElseThrow(() -> new InvitationNotFoundException(id));

    if (!authorizationService.canManageBethel(invitation.getBethel().getId(), authentication)) {
      throw new AccessDeniedException("Você não pode gerenciar este convite.");
    }

    return invitation;
  }
}
