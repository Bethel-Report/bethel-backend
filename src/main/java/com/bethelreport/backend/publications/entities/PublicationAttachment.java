package com.bethelreport.backend.publications.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "publication_attachments")
@Getter
@Setter
@NoArgsConstructor
public class PublicationAttachment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "publication_id", nullable = false)
  private Publication publication;
  @Column(nullable = false, unique = true)
  private String objectKey;
  @Column(nullable = false)
  private String filename;
  @Column(nullable = false, length = 100)
  private String contentType;
  @Column(nullable = false)
  private long size;
  @Column(nullable = false)
  private LocalDateTime createdAt;
}
