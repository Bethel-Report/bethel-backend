package com.bethelreport.backend.reports.dtos;

import com.bethelreport.backend.reports.entities.ReportStatus;
import java.time.LocalDateTime;
import java.util.List;

public record ReportResponse(Long id, Long bethelId, String bethelName, Long formId,
    String formName, ReportStatus status, LocalDateTime submittedAt, LocalDateTime reopenedAt,
    LocalDateTime reopenDeadline, List<ReportAnswerResponse> answers, LocalDateTime createdAt,
    LocalDateTime updatedAt) {
}
