package com.bethelreport.backend.transactions.dtos;

import com.bethelreport.backend.transactions.entities.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransactionResponse(Long id, Long bethelId, Long userId, TransactionType type,
    BigDecimal amount, LocalDate transactionDate, String description, LocalDateTime createdAt,
    LocalDateTime updatedAt) {
}
