package com.bethelreport.backend.users.dtos;

import com.bethelreport.backend.permissions.entities.Role;

public record UserFilters(String email, Long bethelId, Boolean status, Role profile) {
}
