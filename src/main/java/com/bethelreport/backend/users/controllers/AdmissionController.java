package com.bethelreport.backend.users.controllers;

import com.bethelreport.backend.users.dtos.CompleteAdmissionRequest;
import com.bethelreport.backend.users.dtos.UserResponse;
import com.bethelreport.backend.users.services.AdmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admission")
@RequiredArgsConstructor
public class AdmissionController {

  private final AdmissionService admissionService;

  @PostMapping("/complete")
  public ResponseEntity<UserResponse> complete(@AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody CompleteAdmissionRequest request) {

    UserResponse response = admissionService.complete(jwt, request);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
