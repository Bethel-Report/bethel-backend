package com.bethelreport.backend.publications.entities;

import com.bethelreport.backend.bethel.entities.Bethel;
import com.bethelreport.backend.users.entities.User;
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
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "publications")
@Getter
@Setter
@NoArgsConstructor
public class Publication {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(nullable = false, length = 200)
  private String title;
  @Column(nullable = false, columnDefinition = "text")
  private String content;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PublicationType type;
  private LocalDate activityDate;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PublicationAudience audience;
  @ElementCollection
  @CollectionTable(name = "publication_target_bethels")
  @Column(name = "bethel_id", nullable = false)
  private Set<Long> targetBethelIds = new HashSet<>();
  @ElementCollection
  @CollectionTable(name = "publication_target_users")
  @Column(name = "user_id", nullable = false)
  private Set<Long> targetUserIds = new HashSet<>();
  private LocalDateTime displayFrom;
  private LocalDateTime displayUntil;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "author_id", nullable = false, updatable = false)
  private User author;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "bethel_id")
  private Bethel bethel;
  @Column(nullable = false)
  private LocalDateTime timelineAt;
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;
  @Column(nullable = false)
  private LocalDateTime updatedAt;
}
