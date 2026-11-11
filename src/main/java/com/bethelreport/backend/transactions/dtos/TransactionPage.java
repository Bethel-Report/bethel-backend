package com.bethelreport.backend.transactions.dtos;

import java.util.List;
import org.springframework.data.domain.Page;

public record TransactionPage(List<TransactionResponse> content, int number, int size,
    long totalElements, int totalPages) {
  public static TransactionPage from(Page<TransactionResponse> page) {
    return new TransactionPage(page.getContent(), page.getNumber(), page.getSize(),
        page.getTotalElements(), page.getTotalPages());
  }
}
