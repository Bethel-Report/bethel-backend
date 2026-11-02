package com.bethelreport.backend.publications.dtos;

import com.bethelreport.backend.publications.entities.PublicationAudience;
import com.bethelreport.backend.publications.entities.PublicationType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

public record PublicationResponse(Long id, String title, String content, PublicationType type,
    LocalDate activityDate, Long authorId, Long bethelId, PublicationAudience audience,
    Set<Long> targetBethelIds, Set<Long> targetUserIds, LocalDateTime displayFrom,
    LocalDateTime displayUntil, LocalDateTime createdAt, LocalDateTime updatedAt) {
}
