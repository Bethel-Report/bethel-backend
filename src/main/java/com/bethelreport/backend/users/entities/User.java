package com.bethelreport.backend.users.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String email;

  @NotBlank
  private String name;

  @NotNull
  private LocalDate birthday;

  @Column(nullable = false)
  private boolean active;

  @NotBlank
  private String city;

  @NotBlank
  private String address;

  private boolean volunteer;
  private boolean masonicRelationship;
  private boolean member;
  private boolean sponsorship;

}
