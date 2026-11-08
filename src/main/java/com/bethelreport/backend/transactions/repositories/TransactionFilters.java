package com.bethelreport.backend.transactions.repositories;

import com.bethelreport.backend.transactions.entities.FinancialTransaction;
import com.bethelreport.backend.transactions.entities.TransactionType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class TransactionFilters {
  private TransactionFilters() {
  }

  public static Specification<FinancialTransaction> by(Long bethelId, TransactionType type,
      LocalDate startDate, LocalDate endDate) {
    return (root, query, builder) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(builder.equal(root.get("bethel").get("id"), bethelId));
      if (type != null) {
        predicates.add(builder.equal(root.get("type"), type));
      }
      if (startDate != null) {
        predicates.add(builder.greaterThanOrEqualTo(root.get("transactionDate"), startDate));
      }
      if (endDate != null) {
        predicates.add(builder.lessThanOrEqualTo(root.get("transactionDate"), endDate));
      }
      return builder.and(predicates.toArray(Predicate[]::new));
    };
  }
}
