package com.bethelreport.backend.publications.dtos;

import com.bethelreport.backend.publications.entities.PublicationAudience;
import com.bethelreport.backend.publications.entities.PublicationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

public record PublicationRequest(@NotBlank @Size(max = 200) String title,
    @NotBlank @Size(max = 50_000) String content, @NotNull PublicationType type,
    @PastOrPresent LocalDate activityDate, @Positive Long bethelId,
    @NotNull PublicationAudience audience,
    @Size(max = 500) Set<@NotNull @Positive Long> targetBethelIds,
    @Size(max = 500) Set<@NotNull @Positive Long> targetUserIds, LocalDateTime displayFrom,
    LocalDateTime displayUntil) {
}
