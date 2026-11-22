package com.bethelreport.backend.reports.controllers;

import com.bethelreport.backend.reports.dtos.CreateReportRequest;
import com.bethelreport.backend.reports.dtos.ReopenReportRequest;
import com.bethelreport.backend.reports.dtos.ReportResponse;
import com.bethelreport.backend.reports.dtos.SaveAnswersRequest;
import com.bethelreport.backend.reports.services.ReportService;
import com.bethelreport.backend.security.annotations.GrandeConselhoOnly;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
  private final ReportService reportService;

  public ReportController(ReportService reportService) {
    this.reportService = reportService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ReportResponse create(@Valid @RequestBody CreateReportRequest request,
      Authentication authentication) {
    return reportService.create(request, authentication);
  }

  @GetMapping
  public List<ReportResponse> findAll(Authentication authentication) {
    return reportService.findAll(authentication);
  }

  @GetMapping("/{id}")
  public ReportResponse findById(@PathVariable Long id, Authentication authentication) {
    return reportService.findById(id, authentication);
  }

  @PutMapping("/{id}/answers")
  public ReportResponse saveAnswers(@PathVariable Long id,
      @Valid @RequestBody SaveAnswersRequest request, Authentication authentication) {
    return reportService.saveAnswers(id, request, authentication);
  }

  @PatchMapping("/{id}/submit")
  public ReportResponse submit(@PathVariable Long id, Authentication authentication) {
    return reportService.submit(id, authentication);
  }

  @PatchMapping("/{id}/reopen")
  @GrandeConselhoOnly
  public ReportResponse reopen(@PathVariable Long id,
      @Valid @RequestBody ReopenReportRequest request) {
    return reportService.reopen(id, request);
  }
}
