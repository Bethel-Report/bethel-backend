package com.bethelreport.backend.reports.dtos;

import jakarta.validation.constraints.NotNull;

public record CreateReportRequest(@NotNull Long formId) {
}
