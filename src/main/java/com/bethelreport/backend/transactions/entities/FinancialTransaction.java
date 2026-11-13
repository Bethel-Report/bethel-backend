package com.bethelreport.backend.transactions.entities;

import com.bethelreport.backend.bethel.entities.Bethel;
import com.bethelreport.backend.users.entities.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "financial_transactions")
@Getter
@Setter
@NoArgsConstructor
public class FinancialTransaction {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "bethel_id", nullable = false, updatable = false)
  private Bethel bethel;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, updatable = false)
  private User author;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private TransactionType type;
  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;
  @Column(nullable = false)
  private LocalDate transactionDate;
  @Column(nullable = false, length = 2000)
  private String description;
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;
  @Column(nullable = false)
  private LocalDateTime updatedAt;
}
