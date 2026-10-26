
package com.bethelreport.backend.bethel.controllers;

import com.bethelreport.backend.bethel.dtos.BethelResponse;
import com.bethelreport.backend.bethel.dtos.ValidateBethelRequest;
import com.bethelreport.backend.bethel.services.BethelService;
import com.bethelreport.backend.security.annotations.GrandeConselhoOnly;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bethels")
public class BethelController {

  private final BethelService bethelService;

  public BethelController(BethelService bethelService) {
    this.bethelService = bethelService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @GrandeConselhoOnly
  public BethelResponse create(@Valid @RequestBody ValidateBethelRequest request) {

    return bethelService.create(request);
  }

  @GrandeConselhoOnly
  @GetMapping
  public List<BethelResponse> findAll(@RequestParam(required = false) String name) {

    return bethelService.findAll(name);
  }

  @PreAuthorize("@authorizationService.canAccessBethel(#p0, authentication)")
  @GetMapping("/{id}")
  public BethelResponse findById(@PathVariable Long id) {
    return bethelService.findById(id);
  }

  @PreAuthorize("@authorizationService.canManageBethel(#p0, authentication)")
  @PutMapping("/{id}")
  public BethelResponse update(@PathVariable Long id,
      @Valid @RequestBody ValidateBethelRequest request) {

    return bethelService.update(id, request);
  }

  @GrandeConselhoOnly
  @PatchMapping("/{id}/activate")
  public BethelResponse activate(@PathVariable Long id) {
    return bethelService.activate(id);
  }

  @GrandeConselhoOnly
  @PatchMapping("/{id}/deactivate")
  public BethelResponse deactivate(@PathVariable Long id) {
    return bethelService.deactivate(id);
  }
}
