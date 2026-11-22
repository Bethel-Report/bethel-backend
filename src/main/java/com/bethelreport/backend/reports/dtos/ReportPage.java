package com.bethelreport.backend.reports.dtos;

import java.util.List;

public record ReportPage(List<ReportResponse> content, int number, int size, long totalElements,
    int totalPages) {
}
