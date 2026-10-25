package com.bethelreport.backend.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateUserRequest(@NotBlank @Email String email, @NotBlank String name,
    @NotNull LocalDate birthday, @NotBlank String city, @NotBlank String address, boolean volunteer,
    boolean masonicRelationship, boolean member, boolean sponsorship) {
}
