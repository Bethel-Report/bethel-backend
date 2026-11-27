package com.bethelreport.backend.notifications.entities;

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
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notification {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  @Column(nullable = false, length = 255)
  private String title;

  @NotBlank
  @Column(nullable = false, columnDefinition = "text")
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(name = "source_type", nullable = false, length = 20)
  private NotificationType sourceType;

  @Column(name = "resource_id")
  private Long resourceId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "bethel_id")
  private Bethel bethel;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
