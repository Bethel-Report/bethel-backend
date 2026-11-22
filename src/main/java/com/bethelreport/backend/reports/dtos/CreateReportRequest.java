package com.bethelreport.backend.reports.dtos;

import com.bethelreport.backend.reports.entities.ReportType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateReportRequest(@NotNull ReportType type, @NotNull LocalDate periodStart,
    @NotNull LocalDate periodEnd) {
}
