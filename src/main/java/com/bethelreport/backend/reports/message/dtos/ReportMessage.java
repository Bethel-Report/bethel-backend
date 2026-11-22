package com.bethelreport.backend.reports.message.dtos;

import com.bethelreport.backend.reports.entities.ReportType;
import java.time.LocalDate;

public record ReportMessage(Long reportRequestId, Long bethelId, ReportType type,
    LocalDate periodStart, LocalDate periodEnd) {
}
