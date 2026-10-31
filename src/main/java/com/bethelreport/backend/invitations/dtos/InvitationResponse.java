package com.bethelreport.backend.invitations.dtos;

import com.bethelreport.backend.invitations.entities.InvitationStatus;
import java.time.LocalDateTime;

public record InvitationResponse(Long id, String email, InvitationStatus status,
    LocalDateTime createdAt, Long bethelId) {
}
