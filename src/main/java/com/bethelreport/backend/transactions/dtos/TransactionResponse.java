package com.bethelreport.backend.transactions.dtos;

import com.bethelreport.backend.transactions.entities.FinancialTransaction;
import com.bethelreport.backend.transactions.entities.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransactionResponse(Long id, Long bethelId, Long userId, TransactionType type,
    BigDecimal amount, LocalDate transactionDate, String description, LocalDateTime createdAt,
    LocalDateTime updatedAt) {
  public static TransactionResponse from(FinancialTransaction entity) {
    return new TransactionResponse(entity.getId(), entity.getBethel().getId(),
        entity.getAuthor().getId(), entity.getType(), entity.getAmount(),
        entity.getTransactionDate(), entity.getDescription(), entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
