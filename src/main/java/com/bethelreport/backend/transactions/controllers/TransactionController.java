package com.bethelreport.backend.transactions.controllers;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

import com.bethelreport.backend.transactions.dtos.TransactionPage;
import com.bethelreport.backend.transactions.dtos.TransactionRequest;
import com.bethelreport.backend.transactions.dtos.TransactionResponse;
import com.bethelreport.backend.transactions.entities.TransactionType;
import com.bethelreport.backend.transactions.services.TransactionService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/bethels/{bethelId}/transactions")
public class TransactionController {
  private final TransactionService service;

  public TransactionController(TransactionService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public TransactionResponse create(@PathVariable Long bethelId,
      @Valid @RequestBody TransactionRequest request, Authentication authentication) {
    return service.create(bethelId, request, authentication);
  }

  @GetMapping
  public TransactionPage list(@PathVariable Long bethelId,
      @RequestParam(required = false) TransactionType type,
      @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate endDate,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
      Authentication authentication) {
    return TransactionPage
        .from(service.list(bethelId, type, startDate, endDate, page, size, authentication));
  }

  @GetMapping("/{id}")
  public TransactionResponse get(@PathVariable Long bethelId, @PathVariable Long id,
      Authentication authentication) {
    return service.get(bethelId, id, authentication);
  }

  @PutMapping("/{id}")
  public TransactionResponse update(@PathVariable Long bethelId, @PathVariable Long id,
      @Valid @RequestBody TransactionRequest request, Authentication authentication) {
    return service.update(bethelId, id, request, authentication);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long bethelId, @PathVariable Long id,
      Authentication authentication) {
    service.delete(bethelId, id, authentication);
  }
}
