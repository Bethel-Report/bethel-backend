package com.bethelreport.backend.forms.controllers;

import com.bethelreport.backend.forms.dtos.CloneFormRequest;
import com.bethelreport.backend.forms.dtos.CreateFormRequest;
import com.bethelreport.backend.forms.dtos.FormResponse;
import com.bethelreport.backend.forms.dtos.UpdateFormRequest;
import com.bethelreport.backend.forms.services.FormService;
import com.bethelreport.backend.security.annotations.GrandeConselhoOnly;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/forms")
@GrandeConselhoOnly
public class FormController {
  private final FormService formService;

  public FormController(FormService formService) {
    this.formService = formService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public FormResponse create(@Valid @RequestBody CreateFormRequest request) {
    return formService.create(request);
  }

  @GetMapping
  public List<FormResponse> findAll() {
    return formService.findAll();
  }

  @GetMapping("/{id}")
  public FormResponse findById(@PathVariable Long id) {
    return formService.findById(id);
  }

  @PutMapping("/{id}")
  public FormResponse update(@PathVariable Long id, @Valid @RequestBody UpdateFormRequest request) {
    return formService.update(id, request);
  }

  @PostMapping("/{id}/clone")
  @ResponseStatus(HttpStatus.CREATED)
  public FormResponse clone(@PathVariable Long id, @Valid @RequestBody CloneFormRequest request) {
    return formService.clone(id, request);
  }

  @PatchMapping("/{id}/publish")
  public FormResponse publish(@PathVariable Long id) {
    return formService.publish(id);
  }

  @PatchMapping("/{id}/close")
  public FormResponse close(@PathVariable Long id) {
    return formService.close(id);
  }
}
