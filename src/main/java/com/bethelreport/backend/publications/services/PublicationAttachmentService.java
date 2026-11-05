package com.bethelreport.backend.publications.services;

import com.bethelreport.backend.permissions.entities.Permission;
import com.bethelreport.backend.publications.dtos.AttachmentResponse;
import com.bethelreport.backend.publications.entities.Publication;
import com.bethelreport.backend.publications.entities.PublicationAttachment;
import com.bethelreport.backend.publications.mappers.PublicationMapper;
import com.bethelreport.backend.publications.repositories.PublicationAttachmentRepository;
import com.bethelreport.backend.publications.repositories.PublicationRepository;
import com.bethelreport.backend.storage.MinioStorage;
import com.bethelreport.backend.storage.StorageCleanup;
import com.bethelreport.backend.storage.UploadPolicy;
import jakarta.persistence.EntityNotFoundException;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class PublicationAttachmentService {
  private final PublicationRepository publications;
  private final PublicationAttachmentRepository attachments;
  private final PublicationAccess access;
  private final MinioStorage storage;
  private final StorageCleanup cleanup;
  private final UploadPolicy policy;
  private final PublicationMapper mapper;

  public PublicationAttachmentService(PublicationRepository publications,
      PublicationAttachmentRepository attachments, PublicationAccess access, MinioStorage storage,
      StorageCleanup cleanup, UploadPolicy policy, PublicationMapper mapper) {
    this.publications = publications;
    this.attachments = attachments;
    this.access = access;
    this.storage = storage;
    this.cleanup = cleanup;
    this.policy = policy;
    this.mapper = mapper;
  }

  @Transactional
  public AttachmentResponse upload(Long id, MultipartFile file, Authentication authentication) {
    Permission actor = access.actor(authentication);
    Publication publication = locked(id);
    access.requireManagement(actor, access.bethelId(publication));
    UploadPolicy.ValidatedUpload validated = policy.validate(file);
    String category = validated.contentType().startsWith("image/") ? "fotos" : "documentos";
    if (validated.contentType().startsWith("video/")) {
      category = "videos";
    }
    String key = category + "/publicacoes/" + id + "/" + UUID.randomUUID();
    cleanup.registerUpload(key);
    storage.upload(key, new ByteArrayInputStream(validated.bytes()), validated.bytes().length,
        validated.contentType());
    PublicationAttachment attachment = new PublicationAttachment();
    attachment.setPublication(publication);
    attachment.setObjectKey(key);
    attachment.setFilename(validated.filename());
    attachment.setContentType(validated.contentType());
    attachment.setSize(validated.bytes().length);
    attachment.setCreatedAt(LocalDateTime.now());
    AttachmentResponse result = mapper.toResponse(attachments.saveAndFlush(attachment));
    cleanup.cancelUploadCleanup(key);
    return result;
  }

  public List<AttachmentResponse> list(Long id, boolean management, Authentication authentication) {
    authorizeRead(id, management, authentication);
    return attachments.findAllByPublicationIdOrderByIdAsc(id).stream().map(mapper::toResponse)
        .toList();
  }

  public ResponseEntity<byte[]> download(Long id, Long attachmentId, boolean management,
      Authentication authentication) {
    authorizeRead(id, management, authentication);
    PublicationAttachment attachment = find(id, attachmentId);
    byte[] bytes = storage.download(attachment.getObjectKey());
    return ResponseEntity.ok().contentType(MediaType.parseMediaType(attachment.getContentType()))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(attachment.getFilename(), java.nio.charset.StandardCharsets.UTF_8).build()
                .toString())
        .header("X-Content-Type-Options", "nosniff").header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(bytes);
  }

  @Transactional
  public void delete(Long id, Long attachmentId, Authentication authentication) {
    Permission actor = access.actor(authentication);
    Publication publication = locked(id);
    access.requireManagement(actor, access.bethelId(publication));
    PublicationAttachment attachment = find(id, attachmentId);
    cleanup.enqueueDeletion(attachment.getObjectKey());
    attachments.delete(attachment);
  }

  private void authorizeRead(Long id, boolean management, Authentication authentication) {
    Permission actor = access.actor(authentication);
    Publication publication = publications.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Publicação não encontrada."));
    if (management) {
      access.requireManagement(actor, access.bethelId(publication));
    } else {
      access.requireRead(actor, publication);
    }
  }

  private Publication locked(Long id) {
    return publications.findForUpdate(id)
        .orElseThrow(() -> new EntityNotFoundException("Publicação não encontrada."));
  }

  private PublicationAttachment find(Long id, Long attachmentId) {
    return attachments.findByIdAndPublicationId(attachmentId, id)
        .orElseThrow(() -> new EntityNotFoundException("Anexo não encontrado."));
  }
}
