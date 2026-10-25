package com.bethelreport.backend.users.controllers;

import com.bethelreport.backend.users.dtos.CreatePermissionRequest;
import com.bethelreport.backend.users.dtos.PermissionResponse;
import com.bethelreport.backend.users.dtos.UpdatePermissionRequest;
import com.bethelreport.backend.users.services.PermissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/permissions")
public class PermissionController {

  private final PermissionService permissionService;

  public PermissionController(PermissionService permissionService) {
    this.permissionService = permissionService;
  }

  @PostMapping
  public ResponseEntity<PermissionResponse> create(
      @Valid @RequestBody CreatePermissionRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(permissionService.create(request));
  }

  @GetMapping("/users/{userId}")
  public ResponseEntity<PermissionResponse> findByUserId(@PathVariable Long userId) {
    return ResponseEntity.ok(permissionService.findByUserId(userId));
  }

  @GetMapping("/{permissionId}")
  public ResponseEntity<PermissionResponse> findById(@PathVariable Long permissionId) {
    return ResponseEntity.ok(permissionService.findById(permissionId));
  }

  @PatchMapping("/{permissionId}/activate")
  public ResponseEntity<PermissionResponse> activate(@PathVariable Long permissionId) {
    return ResponseEntity.ok(permissionService.activate(permissionId));
  }

  @PatchMapping("/{permissionId}/deactivate")
  public ResponseEntity<PermissionResponse> deactivate(@PathVariable Long permissionId) {
    return ResponseEntity.ok(permissionService.deactivate(permissionId));
  }

  @PutMapping("/{permissionId}")
  public ResponseEntity<PermissionResponse> update(@PathVariable Long permissionId,
      @Valid @RequestBody UpdatePermissionRequest request) {
    return ResponseEntity.ok(permissionService.update(permissionId, request));
  }

  @DeleteMapping("/{permissionId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long permissionId) {
    permissionService.delete(permissionId);
  }
}
