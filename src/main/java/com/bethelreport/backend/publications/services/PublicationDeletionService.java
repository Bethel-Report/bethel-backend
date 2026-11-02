package com.bethelreport.backend.publications.services;

import com.bethelreport.backend.permissions.entities.Permission;
import com.bethelreport.backend.publications.entities.Publication;
import com.bethelreport.backend.publications.entities.PublicationAttachment;
import com.bethelreport.backend.publications.repositories.PublicationAttachmentRepository;
import com.bethelreport.backend.publications.repositories.PublicationRepository;
import com.bethelreport.backend.storage.StorageDeletionQueue;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicationDeletionService {
  private final PublicationRepository publications;
  private final PublicationAttachmentRepository attachments;
  private final PublicationAccess access;
  private final StorageDeletionQueue cleanup;

  public PublicationDeletionService(PublicationRepository publications,
      PublicationAttachmentRepository attachments, PublicationAccess access,
      StorageDeletionQueue cleanup) {
    this.publications = publications;
    this.attachments = attachments;
    this.access = access;
    this.cleanup = cleanup;
  }

  @Transactional
  public void delete(Long id, Authentication authentication) {
    Permission actor = access.actor(authentication);
    Publication publication = publications.findForUpdate(id)
        .orElseThrow(() -> new EntityNotFoundException("Publicação não encontrada."));
    access.requireManagement(actor, access.bethelId(publication));
    List<PublicationAttachment> files = attachments.findAllByPublicationIdOrderByIdAsc(id);
    files.forEach(file -> cleanup.enqueueDeletion(file.getObjectKey()));
    attachments.deleteAll(files);
    publications.delete(publication);
  }
}
