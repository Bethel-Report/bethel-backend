package com.bethelreport.backend.reportgeneration.message.dtos;

public record GenerationResultMessage(Long generationRequestId, String status, String storageKey,
    String failureReason) {}
