package com.bethelreport.backend.users.dtos;

import java.time.LocalDate;

public record UserResponse(Long id, String email, String name, LocalDate birthday, boolean active,
    String city, String address, boolean volunteer, boolean masonicRelationship, boolean member,
    boolean sponsorship) {
}
