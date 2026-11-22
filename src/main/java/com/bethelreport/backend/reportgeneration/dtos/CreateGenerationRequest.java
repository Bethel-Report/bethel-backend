package com.bethelreport.backend.reportgeneration.dtos;

import com.bethelreport.backend.reportgeneration.entities.GenerationType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateGenerationRequest(@NotNull GenerationType type, @NotNull LocalDate periodStart,
    @NotNull LocalDate periodEnd) {
}
