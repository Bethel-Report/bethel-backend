package com.bethelreport.backend.reports.entities;

import com.bethelreport.backend.bethel.entities.Bethel;
import com.bethelreport.backend.forms.entities.Form;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor
public class Report {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "bethel_id", nullable = false)
  private Bethel bethel;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "form_id", nullable = false)
  private Form form;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ReportStatus status;
  @Column(name = "submitted_at")
  private LocalDateTime submittedAt;
  @Column(name = "reopened_at")
  private LocalDateTime reopenedAt;
  @Column(name = "reopen_deadline")
  private LocalDateTime reopenDeadline;
  @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ReportAnswer> answers = new ArrayList<>();
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
}
