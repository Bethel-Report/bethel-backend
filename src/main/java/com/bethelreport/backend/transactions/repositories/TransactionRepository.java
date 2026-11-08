package com.bethelreport.backend.transactions.repositories;

import com.bethelreport.backend.transactions.entities.FinancialTransaction;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TransactionRepository
    extends
      JpaRepository<FinancialTransaction, Long>,
      JpaSpecificationExecutor<FinancialTransaction> {
  Optional<FinancialTransaction> findByIdAndBethelId(Long id, Long bethelId);
}
