package com.bethelreport.backend.forms.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "forms")
@Getter
@Setter
@NoArgsConstructor
public class Form {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(nullable = false, length = 200)
  private String name;
  @Column(length = 2000)
  private String description;
  @Column(name = "reference_year", nullable = false)
  private Integer referenceYear;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private FormStatus status;
  @Column(name = "opening_date")
  private LocalDate openingDate;
  @Column(name = "closing_date")
  private LocalDate closingDate;
  @Column(name = "cloned_from_id")
  private Long clonedFromId;
  @OneToMany(mappedBy = "form", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("displayOrder ASC")
  private List<FormField> fields = new ArrayList<>();
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
}
