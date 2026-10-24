
package com.bethelreport.backend.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bethels")
@Data
@NoArgsConstructor
public class Bethel {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private Integer number;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String city;

  @Column(nullable = false)
  private String address;

  @Column(nullable = false)
  private String email;

  @Column(name = "sponsoring_lodge", nullable = false)
  private String sponsoringLodge;

  @Column(name = "foundation_date")
  private LocalDate foundationDate;

  @Column(nullable = false)
  private boolean active = true;
}
