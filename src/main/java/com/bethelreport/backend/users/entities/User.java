package com.bethelreport.backend.users.entities;

import com.bethelreport.backend.audit.AuditContextEntityListener;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@EntityListeners(AuditContextEntityListener.class)
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(name = "keycloak_user_id", unique = true)
  private String keycloakUserId;

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

  private LocalDateTime deletedAt;
  private LocalDateTime anonymizedAt;

}
