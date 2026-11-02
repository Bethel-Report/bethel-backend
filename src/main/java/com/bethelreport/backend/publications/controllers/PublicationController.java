package com.bethelreport.backend.publications.controllers;

import com.bethelreport.backend.publications.dtos.PublicationPage;
import com.bethelreport.backend.publications.dtos.PublicationRequest;
import com.bethelreport.backend.publications.dtos.PublicationResponse;
import com.bethelreport.backend.publications.entities.PublicationType;
import com.bethelreport.backend.publications.services.PublicationDeletionService;
import com.bethelreport.backend.publications.services.PublicationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/publications")
public class PublicationController {
  private final PublicationService service;
  private final PublicationDeletionService deletion;

  public PublicationController(PublicationService service, PublicationDeletionService deletion) {
    this.service = service;
    this.deletion = deletion;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PublicationResponse create(@Valid @RequestBody PublicationRequest request,
      Authentication authentication) {
    return service.create(request, authentication);
  }

  @GetMapping
  public PublicationPage list(@RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "DESC") Sort.Direction order,
      @RequestParam(required = false) Long bethelId,
      @RequestParam(required = false) PublicationType type,
      @RequestParam(defaultValue = "false") boolean management, Authentication authentication) {
    return PublicationPage
        .from(service.list(page, size, order, bethelId, type, management, authentication));
  }

  @GetMapping("/{id}")
  public PublicationResponse get(@PathVariable Long id,
      @RequestParam(defaultValue = "false") boolean management, Authentication authentication) {
    return service.get(id, management, authentication);
  }

  @PutMapping("/{id}")
  public PublicationResponse update(@PathVariable Long id,
      @Valid @RequestBody PublicationRequest request, Authentication authentication) {
    return service.update(id, request, authentication);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id, Authentication authentication) {
    deletion.delete(id, authentication);
  }
}
