package com.bethelreport.backend.reportgeneration.message.dtos;

import com.bethelreport.backend.reportgeneration.entities.GenerationFormat;
import com.bethelreport.backend.reportgeneration.entities.GenerationType;
import java.time.LocalDate;

public record GenerationMessage(Long generationRequestId, Long bethelId, GenerationType type,
    GenerationFormat format, LocalDate periodStart, LocalDate periodEnd) {
}
