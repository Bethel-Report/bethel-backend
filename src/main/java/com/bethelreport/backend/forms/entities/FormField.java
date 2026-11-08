package com.bethelreport.backend.forms.entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "form_fields")
@Getter
@Setter
@NoArgsConstructor
public class FormField {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "form_id", nullable = false)
  private Form form;
  @Column(nullable = false, length = 300)
  private String label;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private FormFieldType type;
  @Column(nullable = false)
  private boolean required;
  @Column(name = "display_order", nullable = false)
  private Integer displayOrder;
  @ElementCollection
  @CollectionTable(name = "form_field_options", joinColumns = @JoinColumn(name = "field_id"))
  @Column(name = "option_value", length = 500)
  @OrderColumn(name = "option_order")
  private List<String> options = new ArrayList<>();
}
