package com.bethelreport.backend.invitations.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bethelreport.backend.exceptions.DuplicateEmailException;
import com.bethelreport.backend.exceptions.InvitationNotFoundException;
import com.bethelreport.backend.invitations.dtos.CreateInvitationRequest;
import com.bethelreport.backend.invitations.dtos.InvitationResponse;
import com.bethelreport.backend.invitations.entities.Invitation;
import com.bethelreport.backend.invitations.entities.InvitationStatus;
import com.bethelreport.backend.invitations.mappers.InvitationMapper;
import com.bethelreport.backend.invitations.mappers.InvitationMapperImpl;
import com.bethelreport.backend.invitations.repositories.InvitationRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InvitationServiceTest {
  private final InvitationRepository invitationRepository = mock(InvitationRepository.class);
  private final InvitationMapper invitationMapper = new InvitationMapperImpl();
  private final InvitationService invitationService = new InvitationService(invitationRepository,
      invitationMapper);

  @Test
  void createsInvitationSuccessfully() {
    CreateInvitationRequest request = new CreateInvitationRequest("new@example.com");

    when(invitationRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
    when(invitationRepository.save(any(Invitation.class))).thenAnswer(invocation -> {
      Invitation saved = invocation.getArgument(0);
      saved.setId(1L);
      return saved;
    });

    InvitationResponse response = invitationService.createInvitation(request);

    assertEquals(1L, response.id());
    assertEquals("new@example.com", response.email());
    assertEquals(InvitationStatus.PENDING, response.status());
  }

  @Test
  void createInvitationThrowsOnDuplicateEmail() {
    CreateInvitationRequest request = new CreateInvitationRequest("dup@example.com");

    when(invitationRepository.existsByEmailIgnoreCase("dup@example.com")).thenReturn(true);

    assertThrows(DuplicateEmailException.class, () -> invitationService.createInvitation(request));
    verify(invitationRepository, never()).save(any());
  }

  @Test
  void listsAllInvitations() {
    Invitation invitation = buildInvitation(1L, "a@example.com", InvitationStatus.PENDING);

    when(invitationRepository.findAll()).thenReturn(List.of(invitation));

    List<InvitationResponse> result = invitationService.listInvitations();

    assertEquals(1, result.size());
    assertEquals("a@example.com", result.getFirst().email());
  }

  @Test
  void getsInvitationById() {
    Invitation invitation = buildInvitation(1L, "a@example.com", InvitationStatus.PENDING);

    when(invitationRepository.findById(1L)).thenReturn(Optional.of(invitation));

    InvitationResponse response = invitationService.getInvitationById(1L);

    assertEquals(1L, response.id());
    assertEquals("a@example.com", response.email());
  }

  @Test
  void getInvitationByIdThrowsWhenNotFound() {
    when(invitationRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(InvitationNotFoundException.class, () -> invitationService.getInvitationById(99L));
  }

  @Test
  void revokesInvitation() {
    when(invitationRepository.existsById(1L)).thenReturn(true);

    invitationService.revokeInvitation(1L);

    verify(invitationRepository).deleteById(1L);
  }

  @Test
  void revokeInvitationThrowsWhenNotFound() {
    when(invitationRepository.existsById(99L)).thenReturn(false);

    assertThrows(InvitationNotFoundException.class, () -> invitationService.revokeInvitation(99L));
    verify(invitationRepository, never()).deleteById(any());
  }

  private Invitation buildInvitation(Long id, String email, InvitationStatus status) {
    Invitation invitation = new Invitation();
    invitation.setId(id);
    invitation.setEmail(email);
    invitation.setStatus(status);
    invitation.setCreatedAt(LocalDateTime.of(2026, 10, 31, 10, 0));
    return invitation;
  }
}
