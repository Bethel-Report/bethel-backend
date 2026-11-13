package com.bethelreport.backend.transactions.dtos;

import java.util.List;

public record TransactionPage(List<TransactionResponse> content, int number, int size,
    long totalElements, int totalPages) {
}
