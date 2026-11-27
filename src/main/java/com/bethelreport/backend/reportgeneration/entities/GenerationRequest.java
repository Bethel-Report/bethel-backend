package com.bethelreport.backend.reportgeneration.entities;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "report_generation_requests")
@Getter
@Setter
@NoArgsConstructor
public class GenerationRequest {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "bethel_id", nullable = false, updatable = false)
  private Bethel bethel;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "requester_id", nullable = false, updatable = false)
  private User requester;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 20)
  private com.bethelreport.backend.reportgeneration.entities.GenerationType generationType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private GenerationFormat format;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private GenerationStatus status;

  @Column(nullable = false)
  private LocalDate periodStart;

  @Column(nullable = false)
  private LocalDate periodEnd;

  @Column(length = 512)
  private String storageKey;

  @Column(length = 1000)
  private String failureReason;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;
}
