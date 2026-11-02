package com.bethelreport.backend.publications.dtos;

import java.time.LocalDateTime;

public record AttachmentResponse(Long id, String filename, String contentType, long size,
    LocalDateTime createdAt) {
}
