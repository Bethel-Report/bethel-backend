package com.bethelreport.backend.users.dtos;

import com.bethelreport.backend.users.entities.Role;

public record PermissionResponse(Long id, Long userId, Role role, Long bethelId, boolean active) {
}
