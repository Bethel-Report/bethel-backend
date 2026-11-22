package com.bethelreport.backend.reportgeneration.dtos;

import java.util.List;

public record GenerationPage(List<GenerationResponse> content, int number, int size,
    long totalElements, int totalPages) {
}
