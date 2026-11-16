package com.bethelreport.backend.users.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CompleteAdmissionRequest(

    @NotBlank String name,

    @NotNull LocalDate birthday,

    @NotBlank String city,

    @NotBlank String address,

    boolean volunteer, boolean masonicRelationship, boolean member, boolean sponsorship

) {
}
