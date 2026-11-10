package com.bethelreport.backend.forms.dtos;

import com.bethelreport.backend.forms.entities.FormStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record FormResponse(Long id, String name, String description, Integer referenceYear,
    FormStatus status, LocalDate openingDate, LocalDate closingDate, Long clonedFromId,
    Integer version, boolean active, List<FormFieldResponse> fields, LocalDateTime createdAt,
    LocalDateTime updatedAt) {
}
