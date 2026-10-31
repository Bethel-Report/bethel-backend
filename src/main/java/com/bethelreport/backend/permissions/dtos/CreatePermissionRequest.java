package com.bethelreport.backend.permissions.dtos;

import com.bethelreport.backend.permissions.entities.Role;
import jakarta.validation.constraints.NotNull;

public record CreatePermissionRequest(@NotNull Long userId, @NotNull Role role, Long bethelId) {
}
