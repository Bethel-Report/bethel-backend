package com.bethelreport.backend.reports.dtos;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record ReopenReportRequest(@NotNull LocalDateTime reopenDeadline) {
}
