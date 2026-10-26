package com.bethelreport.backend.permissions.dtos;

import com.bethelreport.backend.permissions.entities.Role;

public record PermissionResponse(Long id, Long userId, Role role, Long bethelId, boolean active) {
}
