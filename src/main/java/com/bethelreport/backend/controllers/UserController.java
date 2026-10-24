package com.bethelreport.backend.controllers;

import com.bethelreport.backend.dtos.CreateUserRequest;
import com.bethelreport.backend.dtos.UpdateUserRequest;
import com.bethelreport.backend.dtos.UserResponse;
import com.bethelreport.backend.services.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping
  public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
  }

  @GetMapping
  public ResponseEntity<List<UserResponse>> listUsers() {
    return ResponseEntity.ok(userService.listUsers());
  }

  @GetMapping("/{id}")
  public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
    return ResponseEntity.ok(userService.getUserById(id));
  }

  @PutMapping("/{id}")
  public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
      @Valid @RequestBody UpdateUserRequest request) {
    return ResponseEntity.ok(userService.updateUser(id, request));
  }

  @PatchMapping("/{id}/activate")
  public ResponseEntity<UserResponse> activateUser(@PathVariable Long id) {
    return ResponseEntity.ok(userService.activateUser(id));
  }

  @PatchMapping("/{id}/deactivate")
  public ResponseEntity<UserResponse> deactivateUser(@PathVariable Long id) {
    return ResponseEntity.ok(userService.deactivateUser(id));
  }
}
