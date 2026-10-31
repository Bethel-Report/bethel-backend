package com.bethelreport.backend.invitations.services;

import com.bethelreport.backend.exceptions.DuplicateEmailException;
import com.bethelreport.backend.exceptions.InvitationNotFoundException;
import com.bethelreport.backend.invitations.dtos.CreateInvitationRequest;
import com.bethelreport.backend.invitations.dtos.InvitationResponse;
import com.bethelreport.backend.invitations.entities.Invitation;
import com.bethelreport.backend.invitations.entities.InvitationStatus;
import com.bethelreport.backend.invitations.mappers.InvitationMapper;
import com.bethelreport.backend.invitations.repositories.InvitationRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class InvitationService {
  private final InvitationRepository invitationRepository;
  private final InvitationMapper invitationMapper;

  public InvitationService(InvitationRepository invitationRepository,
      InvitationMapper invitationMapper) {
    this.invitationRepository = invitationRepository;
    this.invitationMapper = invitationMapper;
  }

  public InvitationResponse createInvitation(CreateInvitationRequest request) {
    if (invitationRepository.existsByEmailIgnoreCase(request.email())) {
      throw new DuplicateEmailException(request.email());
    }
    Invitation invitation = invitationMapper.toEntity(request);
    invitation.setStatus(InvitationStatus.PENDING);
    invitation.setCreatedAt(LocalDateTime.now());
    return invitationMapper.toResponse(invitationRepository.save(invitation));
  }

  public List<InvitationResponse> listInvitations() {
    return invitationRepository.findAll().stream().map(invitationMapper::toResponse).toList();
  }

  public InvitationResponse getInvitationById(Long id) {
    return invitationMapper.toResponse(
        invitationRepository.findById(id).orElseThrow(() -> new InvitationNotFoundException(id)));
  }

  public void revokeInvitation(Long id) {
    if (!invitationRepository.existsById(id)) {
      throw new InvitationNotFoundException(id);
    }
    invitationRepository.deleteById(id);
  }
}
