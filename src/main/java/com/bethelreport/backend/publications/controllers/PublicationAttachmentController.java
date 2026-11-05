package com.bethelreport.backend.publications.controllers;

import com.bethelreport.backend.publications.dtos.AttachmentResponse;
import com.bethelreport.backend.publications.services.PublicationAttachmentService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/publications/{id}/attachments")
public class PublicationAttachmentController {
  private final PublicationAttachmentService service;

  public PublicationAttachmentController(PublicationAttachmentService service) {
    this.service = service;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public AttachmentResponse upload(@PathVariable Long id, @RequestPart("file") MultipartFile file,
      Authentication authentication) {
    return service.upload(id, file, authentication);
  }

  @GetMapping
  public List<AttachmentResponse> list(@PathVariable Long id,
      @RequestParam(defaultValue = "false") boolean management, Authentication authentication) {
    return service.list(id, management, authentication);
  }

  @GetMapping("/{attachmentId}")
  public ResponseEntity<byte[]> download(@PathVariable Long id, @PathVariable Long attachmentId,
      @RequestParam(defaultValue = "false") boolean management, Authentication authentication) {
    return service.download(id, attachmentId, management, authentication);
  }

  @DeleteMapping("/{attachmentId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id, @PathVariable Long attachmentId,
      Authentication authentication) {
    service.delete(id, attachmentId, authentication);
  }
}
