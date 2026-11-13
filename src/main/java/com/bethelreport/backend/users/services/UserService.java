package com.bethelreport.backend.users.services;

import com.bethelreport.backend.exceptions.DuplicateEmailException;
import com.bethelreport.backend.exceptions.UserNotFoundException;
import com.bethelreport.backend.invitations.entities.Invitation;
import com.bethelreport.backend.invitations.entities.InvitationStatus;
import com.bethelreport.backend.invitations.repositories.InvitationRepository;
import com.bethelreport.backend.keycloak.services.KeycloakUserService;
import com.bethelreport.backend.permissions.entities.Permission;
import com.bethelreport.backend.permissions.entities.Role;
import com.bethelreport.backend.permissions.repositories.PermissionRepository;
import com.bethelreport.backend.permissions.services.AuthorizationService;
import com.bethelreport.backend.permissions.services.PermissionService;
import com.bethelreport.backend.users.dtos.CreateUserRequest;
import com.bethelreport.backend.users.dtos.UpdateUserRequest;
import com.bethelreport.backend.users.dtos.UserFilters;
import com.bethelreport.backend.users.dtos.UserProfileResponse;
import com.bethelreport.backend.users.dtos.UserProfileResponse.PermissionInfo;
import com.bethelreport.backend.users.dtos.UserResponse;
import com.bethelreport.backend.users.entities.User;
import com.bethelreport.backend.users.mappers.UserMapper;
import com.bethelreport.backend.users.repositories.UserRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class UserService {
  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final PermissionService permissionService;
  private final AuthorizationService authorizationService;
  private final InvitationRepository invitationRepository;
  private final PermissionRepository permissionRepository;
  private final KeycloakUserService keycloakUserService;

  public UserService(UserRepository userRepository, UserMapper userMapper,
      PermissionService permissionService, AuthorizationService authorizationService,
      InvitationRepository invitationRepository, PermissionRepository permissionRepository,
      KeycloakUserService keycloakUserService) {
    this.userRepository = userRepository;
    this.userMapper = userMapper;
    this.permissionService = permissionService;
    this.authorizationService = authorizationService;
    this.invitationRepository = invitationRepository;
    this.permissionRepository = permissionRepository;
    this.keycloakUserService = keycloakUserService;
  }

  @Transactional
  public UserResponse createUser(CreateUserRequest request) {
    if (userRepository.existsByEmailIgnoreCase(request.email())) {
      throw new DuplicateEmailException(request.email());
    }
    User user = userMapper.toEntity(request);
    Optional<Invitation> invitation = invitationRepository
        .findByEmailIgnoreCaseAndStatus(request.email(), InvitationStatus.PENDING);
    if (invitation.isPresent()) {
      String identityId = invitation.get().getKeycloakUserId();
      if (identityId == null || identityId.isBlank()) {
        throw new IllegalStateException("O convite não possui identidade no Keycloak.");
      }
      user.setKeycloakUserId(identityId);
      invitation.get().setStatus(InvitationStatus.ACCEPTED);
      invitationRepository.save(invitation.get());
    } else {
      String identityId = keycloakUserService.provisionUser(request.email(), request.name(), null);
      registerKeycloakCompensation(identityId);
      user.setKeycloakUserId(identityId);
    }

    User saved = userRepository.saveAndFlush(user);
    keycloakUserService.sendInvitationActionsEmail(user.getKeycloakUserId());
    return userMapper.toResponse(saved);
  }

  private void registerKeycloakCompensation(String identityId) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCompletion(int status) {
        if (status == STATUS_ROLLED_BACK) {
          keycloakUserService.deleteUser(identityId);
        }
      }
    });
  }

  public List<UserResponse> listUsers(UserFilters filter) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    UserFilters filters = filter == null ? new UserFilters(null, null, null, null) : filter;
    String email = filters.email() == null || filters.email().isBlank()
        ? null
        : filters.email().trim();

    if (authorizationService.isGrandeConselho(authentication)) {
      return userRepository
          .searchUsers(email, filters.bethelId(), filters.status(), filters.profile()).stream()
          .map(userMapper::toResponse).toList();
    }

    if (authorizationService.hasRole(authentication, Role.CONSELHO_DE_BETHEL)) {
      Long bethelId = authorizationService.getAuthorizedBethelId(authentication);

      return userRepository.searchUsers(email, bethelId, filters.status(), filters.profile())
          .stream().map(userMapper::toResponse).toList();
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

  public UserProfileResponse getAuthenticatedUser(Authentication authentication) {
    if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
      throw new AccessDeniedException("Autenticação inválida.");
    }

    String email = jwtAuth.getToken().getClaimAsString("email");
    User user = userRepository.findByEmailIgnoreCase(email)
        .orElseThrow(() -> new UserNotFoundException(email));

    PermissionInfo permissionInfo = permissionRepository.findByUserEmail(email)
        .filter(Permission::isActive)
        .map(p -> new PermissionInfo(p.getRole(),
            p.getBethel() != null ? p.getBethel().getId() : null,
            p.getBethel() != null ? p.getBethel().getName() : null))
        .orElse(null);

    return new UserProfileResponse(user.getId(), user.getEmail(), user.getName(),
        user.getBirthday(), user.isActive(), user.getCity(), user.getAddress(), user.isVolunteer(),
        user.isMasonicRelationship(), user.isMember(), user.isSponsorship(), permissionInfo);
  }

  private User getAuthorizedUser(Long userId, Authentication authentication) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));

    if (!authorizationService.canManageUsers(authentication, userId)) {
      throw new AccessDeniedException("Você não pode administrar este usuário.");
    }

    return user;
  }

}
