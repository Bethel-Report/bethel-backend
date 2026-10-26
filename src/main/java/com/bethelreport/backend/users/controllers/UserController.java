package com.bethelreport.backend.users.controllers;

import com.bethelreport.backend.security.annotations.CanManageUsers;
import com.bethelreport.backend.users.dtos.CreateUserRequest;
import com.bethelreport.backend.users.dtos.UpdateUserRequest;
import com.bethelreport.backend.users.dtos.UserResponse;
import com.bethelreport.backend.users.services.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

  @CanManageUsers
  @PostMapping
  public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
  }

  @CanManageUsers
  @GetMapping
  public ResponseEntity<List<UserResponse>> listUsers() {
    return ResponseEntity.ok(userService.listUsers());
  }

  @CanManageUsers
  @GetMapping("/{id}")
  public ResponseEntity<UserResponse> getUserById(@PathVariable Long id,
      Authentication authentication) {
    return ResponseEntity.ok(userService.getUserById(id, authentication));
  }

  @CanManageUsers
  @PutMapping("/{id}")
  public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
      @Valid @RequestBody UpdateUserRequest request, Authentication authentication) {
    return ResponseEntity.ok(userService.updateUser(id, request, authentication));
  }

  @CanManageUsers
  @PatchMapping("/{id}/activate")
  public ResponseEntity<UserResponse> activateUser(@PathVariable Long id,
      Authentication authentication) {
    return ResponseEntity.ok(userService.activateUser(id, authentication));
  }

  @CanManageUsers
  @PatchMapping("/{id}/deactivate")
  public ResponseEntity<UserResponse> deactivateUser(@PathVariable Long id,
      Authentication authentication) {
    return ResponseEntity.ok(userService.deactivateUser(id, authentication));
  }
}
