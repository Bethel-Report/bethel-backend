package com.bethelreport.backend.publications.repositories;

import com.bethelreport.backend.publications.entities.PublicationAttachment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicationAttachmentRepository
    extends
      JpaRepository<PublicationAttachment, Long> {
  List<PublicationAttachment> findAllByPublicationIdOrderByIdAsc(Long publicationId);

  Optional<PublicationAttachment> findByIdAndPublicationId(Long id, Long publicationId);
}
