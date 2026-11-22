package com.bethelreport.backend.reports.dtos;

import jakarta.validation.constraints.NotNull;

public record ReportAnswerRequest(@NotNull Long fieldId, String value) {
}
