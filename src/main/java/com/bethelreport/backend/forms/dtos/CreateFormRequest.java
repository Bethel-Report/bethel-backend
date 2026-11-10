package com.bethelreport.backend.forms.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CreateFormRequest(@NotBlank @Size(max = 200) String name,
    @Size(max = 2000) String description, @NotNull Integer referenceYear, LocalDate openingDate,
    LocalDate closingDate, @NotNull @Size(min = 1) List<@Valid FormFieldRequest> fields) {
}
