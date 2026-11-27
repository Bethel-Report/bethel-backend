package com.bethelreport.backend.reportgeneration.dtos;

import com.bethelreport.backend.reportgeneration.entities.GenerationFormat;
import com.bethelreport.backend.reportgeneration.entities.GenerationStatus;
import com.bethelreport.backend.reportgeneration.entities.GenerationType;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record GenerationResponse(Long id, Long bethelId, Long requesterId, GenerationType type,
    GenerationFormat format, GenerationStatus status, LocalDate periodStart, LocalDate periodEnd,
    String failureReason, LocalDateTime createdAt, LocalDateTime updatedAt) {
}
