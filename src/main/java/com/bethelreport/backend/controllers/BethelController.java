
package com.bethelreport.backend.controllers;

import com.bethelreport.backend.dtos.bethel.BethelResponse;
import com.bethelreport.backend.dtos.bethel.CreateBethelRequest;
import com.bethelreport.backend.dtos.bethel.UpdateBethelRequest;
import com.bethelreport.backend.services.BethelService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
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
  public BethelResponse create(@Valid @RequestBody CreateBethelRequest request) {

    return bethelService.create(request);
  }

  @GetMapping
  public List<BethelResponse> findAll(@RequestParam(required = false) String name) {

    return bethelService.findAll(name);
  }

  @GetMapping("/{id}")
  public BethelResponse findById(@PathVariable Long id) {
    return bethelService.findById(id);
  }

  @PutMapping("/{id}")
  public BethelResponse update(@PathVariable Long id,
      @Valid @RequestBody UpdateBethelRequest request) {

    return bethelService.update(id, request);
  }

  @PatchMapping("/{id}/activate")
  public BethelResponse activate(@PathVariable Long id) {
    return bethelService.activate(id);
  }

  @PatchMapping("/{id}/deactivate")
  public BethelResponse deactivate(@PathVariable Long id) {
    return bethelService.deactivate(id);
  }
}
