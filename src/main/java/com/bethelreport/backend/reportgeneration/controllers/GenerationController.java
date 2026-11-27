package com.bethelreport.backend.reportgeneration.controllers;

import com.bethelreport.backend.reportgeneration.dtos.CreateGenerationRequest;
import com.bethelreport.backend.reportgeneration.dtos.GenerationPage;
import com.bethelreport.backend.reportgeneration.dtos.GenerationResponse;
import com.bethelreport.backend.reportgeneration.entities.GenerationFormat;
import com.bethelreport.backend.reportgeneration.services.GenerationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bethels/{bethelId}/report-generations")
public class GenerationController {

  private final GenerationService service;

  public GenerationController(GenerationService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public GenerationResponse create(@PathVariable Long bethelId,
      @Valid @RequestBody CreateGenerationRequest request, Authentication authentication) {
    return service.create(bethelId, request, authentication);
  }

  @GetMapping("/{id}")
  public GenerationResponse getStatus(@PathVariable Long bethelId, @PathVariable Long id,
      Authentication authentication) {
    return service.getStatus(bethelId, id, authentication);
  }

  @GetMapping
  public GenerationPage list(@PathVariable Long bethelId,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
      Authentication authentication) {
    return service.list(bethelId, page, size, authentication);
  }

  @GetMapping("/{id}/download")
  public ResponseEntity<byte[]> download(@PathVariable Long bethelId, @PathVariable Long id,
      Authentication authentication) {
    GenerationService.DownloadResult result = service.download(bethelId, id, authentication);
    String extension = result.format() == GenerationFormat.XLSX ? "xlsx" : "pdf";
    MediaType mediaType = result.format() == GenerationFormat.XLSX
        ? MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        : MediaType.APPLICATION_PDF;
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"report-" + id + "." + extension + "\"")
        .contentType(mediaType).body(result.content());
  }
}
