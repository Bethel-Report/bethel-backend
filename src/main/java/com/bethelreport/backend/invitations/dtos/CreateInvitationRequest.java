package com.bethelreport.backend.invitations.dtos;

import com.bethelreport.backend.permissions.entities.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateInvitationRequest(@NotBlank @Email String email, Long bethelId,
    @NotNull Role role) {
}
