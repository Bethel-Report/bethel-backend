package com.bethelreport.backend.users.services;

import com.bethelreport.backend.exceptions.DuplicateEmailException;
import com.bethelreport.backend.exceptions.UserNotFoundException;
import com.bethelreport.backend.permissions.dtos.PermissionResponse;
import com.bethelreport.backend.permissions.entities.Role;
import com.bethelreport.backend.permissions.services.AuthorizationService;
import com.bethelreport.backend.permissions.services.PermissionService;
import com.bethelreport.backend.users.dtos.CreateUserRequest;
import com.bethelreport.backend.users.dtos.UpdateUserRequest;
import com.bethelreport.backend.users.dtos.UserResponse;
import com.bethelreport.backend.users.entities.User;
import com.bethelreport.backend.users.mappers.UserMapper;
import com.bethelreport.backend.users.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final PermissionService permissionService;
  private final AuthorizationService authorizationService;

  public UserService(UserRepository userRepository, UserMapper userMapper,
      PermissionService permissionService, AuthorizationService authorizationService) {
    this.userRepository = userRepository;
    this.userMapper = userMapper;
    this.permissionService = permissionService;
    this.authorizationService = authorizationService;
  }

  public UserResponse createUser(CreateUserRequest request) {
    if (userRepository.existsByEmailIgnoreCase(request.email())) {
      throw new DuplicateEmailException(request.email());
    }
    User user = userMapper.toEntity(request);
    return userMapper.toResponse(userRepository.save(user));
  }

  public List<UserResponse> listUsers() {
    return userRepository.findAll().stream().map(userMapper::toResponse).toList();
  }

  public UserResponse getUserById(Long id) {
    return userMapper.toResponse(findUserOrThrow(id));
  }

  @Transactional
  public UserResponse updateUser(Long id, UpdateUserRequest request) {
    requireCanManageUser(id);
    User user = findUserOrThrow(id);
    if (!user.getEmail().equalsIgnoreCase(request.email())
        && userRepository.existsByEmailIgnoreCase(request.email())) {
      throw new DuplicateEmailException(request.email());
    }
    userMapper.updateEntity(request, user);
    return userMapper.toResponse(userRepository.save(user));
  }

  @Transactional
  public UserResponse activateUser(Long id) {
    requireCanManageUser(id);
    User user = findUserOrThrow(id);
    user.setActive(true);
    return userMapper.toResponse(userRepository.save(user));
  }

  @Transactional
  public UserResponse deactivateUser(Long id) {
    requireCanManageUser(id);
    User user = findUserOrThrow(id);
    user.setActive(false);
    User savedUser = userRepository.save(user);
    permissionService.deactivateByUserId(id);
    return userMapper.toResponse(savedUser);
  }

  private User findUserOrThrow(Long id) {
    return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
  }

  private void requireCanManageUser(Long userId) {
    try {
      PermissionResponse permission = permissionService.findByUserId(userId);
      if (permission == null) {
        requireCanAssignRole(Role.GRANDE_CONSELHO, null);
      } else {
        requireCanAssignRole(permission.role(), permission.bethelId());
      }
    } catch (EntityNotFoundException exception) {
      requireCanAssignRole(Role.GRANDE_CONSELHO, null);
    }
  }

  private void requireCanAssignRole(Role role, Long bethelId) {
    if (!authorizationService.canAssignRole(SecurityContextHolder.getContext().getAuthentication(),
        role, bethelId)) {
      throw new AccessDeniedException("Você não tem permissão para gerenciar este usuário");
    }
  }
}
