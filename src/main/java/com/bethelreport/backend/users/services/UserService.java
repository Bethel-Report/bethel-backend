package com.bethelreport.backend.users.services;

import com.bethelreport.backend.exceptions.DuplicateEmailException;
import com.bethelreport.backend.permissions.entities.Role;
import com.bethelreport.backend.permissions.repositories.PermissionRepository;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final PermissionService permissionService;
  private final AuthorizationService authorizationService;
  private final PermissionRepository permissionRepository;

  public UserService(UserRepository userRepository, UserMapper userMapper,
      PermissionService permissionService, AuthorizationService authorizationService,
      PermissionRepository permissionRepository) {
    this.userRepository = userRepository;
    this.userMapper = userMapper;
    this.permissionService = permissionService;
    this.authorizationService = authorizationService;
    this.permissionRepository = permissionRepository;
  }

  public UserResponse createUser(CreateUserRequest request) {
    if (userRepository.existsByEmailIgnoreCase(request.email())) {
      throw new DuplicateEmailException(request.email());
    }
    User user = userMapper.toEntity(request);
    return userMapper.toResponse(userRepository.save(user));
  }

  public List<UserResponse> listUsers() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authorizationService.isGrandeConselho(authentication)) {
      return userRepository.findAll().stream().map(userMapper::toResponse).toList();
    }

    if (authorizationService.hasRole(authentication, Role.CONSELHO_DE_BETHEL)) {

      Long bethelId = authorizationService.getAuthorizedBethelId(authentication);

      return userRepository.findAllByBethelId(bethelId).stream().map(userMapper::toResponse)
          .toList();
    }

    throw new AccessDeniedException("Você não possui permissão para listar usuários.");
  }

  public UserResponse getUserById(Long id, Authentication authentication) {
    return userMapper.toResponse(getAuthorizedUser(id, authentication));
  }

  @Transactional
  public UserResponse updateUser(Long id, UpdateUserRequest request,
      Authentication authentication) {

    User user = getAuthorizedUser(id, authentication);

    if (!user.getEmail().equalsIgnoreCase(request.email())
        && userRepository.existsByEmailIgnoreCase(request.email())) {
      throw new DuplicateEmailException(request.email());
    }

    userMapper.updateEntity(request, user);
    return userMapper.toResponse(userRepository.save(user));
  }

  @Transactional
  public UserResponse activateUser(Long id, Authentication authentication) {
    User user = getAuthorizedUser(id, authentication);
    user.setActive(true);

    return userMapper.toResponse(userRepository.save(user));
  }

  @Transactional
  public UserResponse deactivateUser(Long id, Authentication authentication) {

    User user = getAuthorizedUser(id, authentication);

    user.setActive(false);
    User savedUser = userRepository.save(user);
    permissionService.deactivateByUserId(id);
    return userMapper.toResponse(savedUser);
  }

  private User getAuthorizedUser(Long userId, Authentication authentication) {

    if (!authorizationService.canManageUsers(authentication, userId)) {
      throw new AccessDeniedException("Você não pode administrar este usuário.");
    }

    return userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));
  }

}
