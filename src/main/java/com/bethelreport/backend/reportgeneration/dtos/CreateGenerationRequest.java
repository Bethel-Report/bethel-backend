package com.bethelreport.backend.reportgeneration.dtos;

import com.bethelreport.backend.reportgeneration.entities.GenerationFormat;
import com.bethelreport.backend.reportgeneration.entities.GenerationType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateGenerationRequest(@NotNull GenerationType type, GenerationFormat format,
    @NotNull LocalDate periodStart, @NotNull LocalDate periodEnd) {
}
