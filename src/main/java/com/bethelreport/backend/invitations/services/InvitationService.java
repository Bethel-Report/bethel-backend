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
import com.bethelreport.backend.permissions.entities.Role;
import com.bethelreport.backend.permissions.services.AuthorizationService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InvitationService {
  private final InvitationRepository invitationRepository;
  private final InvitationMapper invitationMapper;
  private final AuthorizationService authorizationService;
  private final BethelRepository bethelRepository;

  public InvitationService(InvitationRepository invitationRepository,
      InvitationMapper invitationMapper, AuthorizationService authorizationService,
      BethelRepository bethelRepository) {
    this.invitationRepository = invitationRepository;
    this.invitationMapper = invitationMapper;
    this.authorizationService = authorizationService;
    this.bethelRepository = bethelRepository;
  }

  public InvitationResponse createInvitation(CreateInvitationRequest request,
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

    final Bethel bethel = bethelRepository.getReferenceById(request.bethelId());
    Invitation invitation = invitationMapper.toEntity(request);
    invitation.setStatus(InvitationStatus.PENDING);
    invitation.setCreatedAt(LocalDateTime.now());
    invitation.setBethel(bethel);
    return invitationMapper.toResponse(invitationRepository.save(invitation));
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
