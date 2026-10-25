package com.bethelreport.backend.users.dtos;

import com.bethelreport.backend.users.entities.Role;
import jakarta.validation.constraints.NotNull;

public record CreatePermissionRequest(@NotNull Long userId, @NotNull Role role, Long bethelId) {
}
