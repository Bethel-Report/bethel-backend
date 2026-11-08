package com.bethelreport.backend.forms.dtos;

import com.bethelreport.backend.forms.entities.FormFieldType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record FormFieldRequest(@NotBlank @Size(max = 300) String label, @NotNull FormFieldType type,
    @NotNull Boolean required, @NotNull @Positive Integer displayOrder,
    List<@NotBlank String> options) {
}
