package com.bethelreport.backend.users.dtos;

import com.bethelreport.backend.users.entities.Role;
import jakarta.validation.constraints.NotNull;

public record UpdatePermissionRequest(@NotNull Role role, Long bethelId) {
}
