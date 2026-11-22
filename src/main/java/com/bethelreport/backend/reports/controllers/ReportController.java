package com.bethelreport.backend.reports.controllers;

import com.bethelreport.backend.reports.dtos.CreateReportRequest;
import com.bethelreport.backend.reports.dtos.ReportPage;
import com.bethelreport.backend.reports.dtos.ReportResponse;
import com.bethelreport.backend.reports.services.ReportService;
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
@RequestMapping("/api/bethels/{bethelId}/reports")
public class ReportController {

  private final ReportService service;

  public ReportController(ReportService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ReportResponse create(@PathVariable Long bethelId,
      @Valid @RequestBody CreateReportRequest request, Authentication authentication) {
    return service.create(bethelId, request, authentication);
  }

  @GetMapping("/{id}")
  public ReportResponse getStatus(@PathVariable Long bethelId, @PathVariable Long id,
      Authentication authentication) {
    return service.getStatus(bethelId, id, authentication);
  }

  @GetMapping
  public ReportPage list(@PathVariable Long bethelId, @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size, Authentication authentication) {
    return service.list(bethelId, page, size, authentication);
  }

  @GetMapping("/{id}/download")
  public ResponseEntity<byte[]> download(@PathVariable Long bethelId, @PathVariable Long id,
      Authentication authentication) {
    byte[] content = service.download(bethelId, id, authentication);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report-" + id + ".pdf\"")
        .contentType(MediaType.APPLICATION_PDF).body(content);
  }
}
