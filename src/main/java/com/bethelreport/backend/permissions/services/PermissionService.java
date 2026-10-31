package com.bethelreport.backend.permissions.services;

import com.bethelreport.backend.bethel.entities.Bethel;
import com.bethelreport.backend.bethel.repositories.BethelRepository;
import com.bethelreport.backend.permissions.dtos.CreatePermissionRequest;
import com.bethelreport.backend.permissions.dtos.PermissionResponse;
import com.bethelreport.backend.permissions.dtos.UpdatePermissionRequest;
import com.bethelreport.backend.permissions.entities.Permission;
import com.bethelreport.backend.permissions.entities.Role;
import com.bethelreport.backend.permissions.mappers.PermissionMapper;
import com.bethelreport.backend.permissions.repositories.PermissionRepository;
import com.bethelreport.backend.users.entities.User;
import com.bethelreport.backend.users.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PermissionService {

  private final PermissionRepository permissionRepository;
  private final UserRepository userRepository;
  private final BethelRepository bethelRepository;
  private final PermissionMapper permissionMapper;
  private final AuthorizationService authorizationService;

  @Transactional
  public PermissionResponse create(CreatePermissionRequest request) {
    requireCanAssignRole(request.role(), request.bethelId());
    User user = userRepository.findById(request.userId())
        .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

    if (!user.isActive()) {
      throw new IllegalArgumentException("Não é possível atribuir perfil a usuário inativo");
    }

    if (permissionRepository.existsByUserId(user.getId())) {
      throw new IllegalStateException("O usuário já possui um perfil associado");
    }

    final Bethel bethel = validateAndGetBethel(request.role(), request.bethelId());

    Permission permission = permissionMapper.toEntity(request);
    permission.setUser(user);
    permission.setBethel(bethel);

    return permissionMapper.toResponse(permissionRepository.save(permission));
  }

  @Transactional
  public List<PermissionResponse> findAdministratorsByBethelId(Long bethelId) {
    if (!authorizationService
        .isGrandeConselho(SecurityContextHolder.getContext().getAuthentication())) {
      throw new AccessDeniedException("Apenas o Grande Conselho pode consultar responsáveis");
    }
    if (!bethelRepository.existsById(bethelId)) {
      throw new EntityNotFoundException("Bethel não encontrado");
    }
    return permissionRepository
        .findAllByBethelIdAndRoleAndActiveTrueAndUserActiveTrueOrderByIdAsc(bethelId,
            Role.CONSELHO_DE_BETHEL)
        .stream().map(permissionMapper::toResponse).toList();
  }

  public PermissionResponse findByUserId(Long userId) {
    Permission permission = permissionRepository.findByUserId(userId)
        .orElseThrow(() -> new EntityNotFoundException("Permissão do usuário não encontrada"));
    requireCanAssignRole(permission.getRole(), getBethelId(permission));
    return permissionMapper.toResponse(permission);
  }

  public PermissionResponse findById(Long permissionId) {
    Permission permission = findPermissionOrThrow(permissionId);
    requireCanAssignRole(permission.getRole(), getBethelId(permission));
    return permissionMapper.toResponse(permission);
  }

  @Transactional
  public PermissionResponse activate(Long permissionId) {
    Permission permission = findPermissionOrThrow(permissionId);
    requireCanAssignRole(permission.getRole(), getBethelId(permission));

    if (permission.isActive()) {
      throw new IllegalArgumentException("Permissão já ativa");
    } else {
      permission.setActive(true);
    }

    return permissionMapper.toResponse(permissionRepository.save(permission));
  }

  @Transactional
  public PermissionResponse deactivate(Long permissionId) {
    Permission permission = findPermissionOrThrow(permissionId);
    requireCanAssignRole(permission.getRole(), getBethelId(permission));

    if (!permission.isActive()) {
      throw new IllegalArgumentException("Permissão já desativada");
    } else {
      permission.setActive(false);
    }

    return permissionMapper.toResponse(permissionRepository.save(permission));
  }

  @Transactional
  public void deactivateByUserId(Long userId) {
    permissionRepository.findByUserId(userId).ifPresent(this::deactivatePermission);
  }

  @Transactional
  public void deactivateByBethelId(Long bethelId) {
    List<Permission> permissions = permissionRepository.findAllByBethelId(bethelId);
    permissions.forEach(this::deactivatePermission);
  }

  @Transactional
  public PermissionResponse update(Long permissionId, UpdatePermissionRequest request) {

    Permission permission = findPermissionOrThrow(permissionId);
    requireCanAssignRole(permission.getRole(), getBethelId(permission));

    final Bethel bethel = validateAndGetBethel(request.role(), request.bethelId());
    requireCanAssignRole(request.role(), request.bethelId());

    permissionMapper.updateEntity(request, permission);
    permission.setBethel(bethel);

    return permissionMapper.toResponse(permissionRepository.save(permission));
  }

  @Transactional
  public void delete(Long permissionId) {
    Permission permission = findPermissionOrThrow(permissionId);
    requireCanAssignRole(permission.getRole(), getBethelId(permission));

    permissionRepository.delete(permission);
  }

  private Bethel validateAndGetBethel(Role role, Long bethelId) {

    if (role == null) {
      throw new IllegalArgumentException("O perfil é obrigatório");
    }

    if (role == Role.GRANDE_CONSELHO) {
      if (bethelId != null) {
        throw new IllegalArgumentException("Grande Conselho não deve estar associado a um Bethel");
      }

      return null;
    }

    if (bethelId == null) {
      throw new IllegalArgumentException("Este perfil exige um Bethel");
    }

    return findActiveBethel(bethelId);
  }

  private Bethel findActiveBethel(Long bethelId) {
    Bethel bethel = bethelRepository.findById(bethelId)
        .orElseThrow(() -> new EntityNotFoundException("Bethel não encontrado"));
    if (!bethel.isActive()) {
      throw new IllegalArgumentException("Não é possível vincular usuário a Bethel inativo");
    }
    return bethel;
  }

  private Permission findPermissionOrThrow(Long permissionId) {
    return permissionRepository.findById(permissionId)
        .orElseThrow(() -> new EntityNotFoundException("Permissão não encontrada"));
  }

  private void deactivatePermission(Permission permission) {
    requireCanAssignRole(permission.getRole(), getBethelId(permission));
    permission.setActive(false);
    permissionRepository.save(permission);
  }

  private void requireCanAssignRole(Role role, Long bethelId) {
    if (!authorizationService.canAssignRole(SecurityContextHolder.getContext().getAuthentication(),
        role, bethelId)) {
      throw new AccessDeniedException("Você não tem permissão para gerenciar este perfil");
    }
  }

  private Long getBethelId(Permission permission) {
    return permission.getBethel() == null ? null : permission.getBethel().getId();
  }
}
