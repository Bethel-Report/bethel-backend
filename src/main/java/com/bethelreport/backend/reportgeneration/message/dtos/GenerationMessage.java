package com.bethelreport.backend.reportgeneration.message.dtos;

import com.bethelreport.backend.reportgeneration.entities.GenerationType;
import java.time.LocalDate;

public record GenerationMessage(Long generationRequestId, Long bethelId, GenerationType type,
    LocalDate periodStart, LocalDate periodEnd) {
}
