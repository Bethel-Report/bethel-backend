package com.bethelreport.backend.transactions.dtos;

import com.bethelreport.backend.transactions.entities.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(@NotNull TransactionType type,
    @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
    @NotNull LocalDate transactionDate, @NotBlank @Size(max = 2000) String description) {
}
