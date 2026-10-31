package com.bethelreport.backend.invitations.entities;

import com.bethelreport.backend.bethel.entities.Bethel;
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
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "invitations")
@Data
@NoArgsConstructor
public class Invitation {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private InvitationStatus status;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "bethel_id", nullable = false)
  private Bethel bethel;

  @Column(nullable = false)
  private LocalDateTime createdAt;
}
