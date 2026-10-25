package com.bethelreport.backend.users.services;

import com.bethelreport.backend.bethel.entities.Bethel;
import com.bethelreport.backend.bethel.repositories.BethelRepository;
import com.bethelreport.backend.users.dtos.CreatePermissionRequest;
import com.bethelreport.backend.users.dtos.PermissionResponse;
import com.bethelreport.backend.users.dtos.UpdatePermissionRequest;
import com.bethelreport.backend.users.entities.Permission;
import com.bethelreport.backend.users.entities.Role;
import com.bethelreport.backend.users.entities.User;
import com.bethelreport.backend.users.mappers.PermissionMapper;
import com.bethelreport.backend.users.repositories.PermissionRepository;
import com.bethelreport.backend.users.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PermissionService {

  private final PermissionRepository permissionRepository;
  private final UserRepository userRepository;
  private final BethelRepository bethelRepository;
  private final PermissionMapper permissionMapper;

  @Transactional
  public PermissionResponse create(CreatePermissionRequest request) {
    User user = userRepository.findById(request.userId())
        .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

    if (!user.isActive()) {
      throw new IllegalArgumentException("Não é possível atribuir perfil a usuário inativo");
    }

    if (permissionRepository.existsByUserId(user.getId())) {
      throw new IllegalStateException("O usuário já possui um perfil associado");
    }

    Bethel bethel = validateAndGetBethel(request.role(), request.bethelId());

    Permission permission = permissionMapper.toEntity(request);
    permission.setUser(user);
    permission.setBethel(bethel);

    return permissionMapper.toResponse(permissionRepository.save(permission));
  }

  public PermissionResponse findByUserId(Long userId) {
    Permission permission = permissionRepository.findByUserId(userId)
        .orElseThrow(() -> new EntityNotFoundException("Permissão do usuário não encontrada"));
    return permissionMapper.toResponse(permission);
  }

  public PermissionResponse findById(Long permissionId) {
    return permissionMapper.toResponse(findPermissionOrThrow(permissionId));
  }

  @Transactional
  public PermissionResponse activate(Long permissionId) {
    Permission permission = findPermissionOrThrow(permissionId);

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

    Bethel bethel = validateAndGetBethel(request.role(), request.bethelId());

    permissionMapper.updateEntity(request, permission);
    permission.setBethel(bethel);

    return permissionMapper.toResponse(permissionRepository.save(permission));
  }

  @Transactional
  public void delete(Long permissionId) {
    Permission permission = findPermissionOrThrow(permissionId);

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

    return bethelRepository.findById(bethelId)
        .orElseThrow(() -> new EntityNotFoundException("Bethel não encontrado"));
  }

  private Permission findPermissionOrThrow(Long permissionId) {
    return permissionRepository.findById(permissionId)
        .orElseThrow(() -> new EntityNotFoundException("Permissão não encontrada"));
  }

  private void deactivatePermission(Permission permission) {
    permission.setActive(false);
    permissionRepository.save(permission);
  }
}
