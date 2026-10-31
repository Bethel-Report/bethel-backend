package com.bethelreport.backend.permissions.services;

import com.bethelreport.backend.permissions.entities.Permission;
import com.bethelreport.backend.permissions.entities.Role;
import com.bethelreport.backend.permissions.repositories.PermissionRepository;
import java.util.Arrays;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service("authorizationService")
public class AuthorizationService {

  private final PermissionRepository permissionRepository;

  public AuthorizationService(PermissionRepository permissionRepository) {
    this.permissionRepository = permissionRepository;
  }

  public boolean hasRole(Authentication authentication, Role... expectedRoles) {

    Permission permission = findPermission(authentication);

    if (permission == null || expectedRoles == null) {
      return false;
    }

    return Arrays.stream(expectedRoles).anyMatch(role -> role == permission.getRole());
  }

  public boolean isGrandeConselho(Authentication authentication) {

    return hasRole(authentication, Role.GRANDE_CONSELHO);
  }

  public boolean canAccessBethel(Long bethelId, Authentication authentication) {

    Permission permission = findPermission(authentication);

    if (permission == null || bethelId == null) {
      return false;
    }

    return permission.getRole() == Role.GRANDE_CONSELHO
        || (permission.getBethel() != null && bethelId.equals(permission.getBethel().getId()));
  }

  public boolean canManageBethel(Long bethelId, Authentication authentication) {

    Permission permission = findPermission(authentication);

    if (permission == null || bethelId == null) {
      return false;
    }

    return permission.getRole() == Role.GRANDE_CONSELHO
        || (permission.getRole() == Role.CONSELHO_DE_BETHEL && permission.getBethel() != null
            && bethelId.equals(permission.getBethel().getId()));
  }

  public boolean canAssignRole(Authentication authentication, Role requestedRole,
      Long requestedBethelId) {

    Permission actor = findPermission(authentication);

    if (actor == null || requestedRole == null) {
      return false;
    }

    if (actor.getRole() == Role.GRANDE_CONSELHO) {
      return requestedRole == Role.GRANDE_CONSELHO
          ? requestedBethelId == null
          : requestedBethelId != null;
    }

    if (requestedRole == Role.GRANDE_CONSELHO) {
      return false;
    }

    if (actor.getRole() != Role.CONSELHO_DE_BETHEL || actor.getBethel() == null
        || requestedBethelId == null) {
      return false;
    }

    return requestedBethelId.equals(actor.getBethel().getId());
  }

  public boolean canManageUsers(Authentication authentication) {
    return hasRole(authentication, Role.GRANDE_CONSELHO, Role.CONSELHO_DE_BETHEL);
  }

  public boolean canManageUsers(Authentication authentication, Long targetUserId) {
    Permission permission = findPermission(authentication);

    if (permission == null) {
      return false;
    }

    if (permission.getRole() == Role.GRANDE_CONSELHO) {
      return true;
    }

    if (permission.getRole() != Role.CONSELHO_DE_BETHEL || permission.getBethel() == null) {
      return false;
    }

    return permissionRepository.findByUserId(targetUserId)
        .map(targetPermission -> targetPermission.getBethel() != null
            && permission.getBethel().getId().equals(targetPermission.getBethel().getId()))
        .orElse(false);
  }

  public Long getAuthorizedBethelId(Authentication authentication) {

    Permission permission = findPermission(authentication);

    if (permission == null) {
      throw new AccessDeniedException("Permissão ativa não encontrada.");
    }

    if (permission.getRole() != Role.CONSELHO_DE_BETHEL || permission.getBethel() == null) {
      throw new AccessDeniedException("Usuário não possui acesso administrativo a um Bethel.");
    }

    return permission.getBethel().getId();
  }

  private Permission findPermission(Authentication authentication) {

    if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
      return null;
    }

    String email = jwtAuth.getToken().getClaimAsString("email");

    if (email == null || email.isBlank()) {
      return null;
    }

    return permissionRepository.findByUserEmail(email).filter(Permission::isActive).orElse(null);
  }
}
