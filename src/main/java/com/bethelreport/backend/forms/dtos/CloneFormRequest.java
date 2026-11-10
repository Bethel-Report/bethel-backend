package com.bethelreport.backend.forms.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CloneFormRequest(@NotBlank @Size(max = 200) String name,
    @NotNull Integer referenceYear, LocalDate openingDate, LocalDate closingDate) {
}
