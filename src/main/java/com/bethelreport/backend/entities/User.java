package com.bethelreport.backend.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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
