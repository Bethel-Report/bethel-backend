package com.bethelreport.backend.users.dtos;

import com.bethelreport.backend.permissions.entities.Role;
import java.time.LocalDate;

public record UserProfileResponse(Long id, String email, String name, LocalDate birthday,
    boolean active, String city, String address, boolean volunteer, boolean masonicRelationship,
    boolean member, boolean sponsorship, PermissionInfo permission) {

  public record PermissionInfo(Role role, Long bethelId, String bethelName) {
  }
}
