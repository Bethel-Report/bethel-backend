package com.bethelreport.backend.invitations.controllers;

import com.bethelreport.backend.invitations.dtos.CreateInvitationRequest;
import com.bethelreport.backend.invitations.dtos.InvitationResponse;
import com.bethelreport.backend.invitations.services.InvitationService;
import com.bethelreport.backend.security.annotations.CanManageUsers;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invitations")
public class InvitationController {
  private final InvitationService invitationService;

  public InvitationController(InvitationService invitationService) {
    this.invitationService = invitationService;
  }

  @CanManageUsers
  @PostMapping
  public ResponseEntity<InvitationResponse> createInvitation(
      @Valid @RequestBody CreateInvitationRequest request, Authentication authentication) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(invitationService.createInvitation(request, authentication));
  }

  @CanManageUsers
  @GetMapping
  public ResponseEntity<List<InvitationResponse>> listInvitations() {
    return ResponseEntity.ok(invitationService.listInvitations());
  }

  @CanManageUsers
  @GetMapping("/{id}")
  public ResponseEntity<InvitationResponse> getInvitationById(@PathVariable Long id,
      Authentication authentication) {
    return ResponseEntity.ok(invitationService.getInvitationById(id, authentication));
  }

  @CanManageUsers
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void revokeInvitation(@PathVariable Long id, Authentication authentication) {
    invitationService.revokeInvitation(id, authentication);
  }
}
