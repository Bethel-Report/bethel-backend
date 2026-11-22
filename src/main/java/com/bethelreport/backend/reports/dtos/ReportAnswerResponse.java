package com.bethelreport.backend.reports.dtos;

import com.bethelreport.backend.forms.entities.FormFieldType;
import java.time.LocalDateTime;

public record ReportAnswerResponse(Long id, Long fieldId, String fieldLabel,
    FormFieldType fieldType, String value, LocalDateTime updatedAt) {
}
