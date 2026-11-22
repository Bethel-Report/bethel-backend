package com.bethelreport.backend.reports.dtos;

import com.bethelreport.backend.reports.entities.ReportStatus;
import com.bethelreport.backend.reports.entities.ReportType;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReportResponse(Long id, Long bethelId, Long requesterId, ReportType type,
    ReportStatus status, LocalDate periodStart, LocalDate periodEnd, String failureReason,
    LocalDateTime createdAt, LocalDateTime updatedAt) {
}
