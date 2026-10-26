package com.bethelreport.backend.permissions.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.bethelreport.backend.bethel.entities.Bethel;
import com.bethelreport.backend.bethel.repositories.BethelRepository;
import com.bethelreport.backend.permissions.dtos.CreatePermissionRequest;
import com.bethelreport.backend.permissions.dtos.PermissionResponse;
import com.bethelreport.backend.permissions.dtos.UpdatePermissionRequest;
import com.bethelreport.backend.permissions.entities.Permission;
import com.bethelreport.backend.permissions.entities.Role;
import com.bethelreport.backend.users.entities.User;
import com.bethelreport.backend.permissions.mappers.PermissionMapper;
import com.bethelreport.backend.permissions.repositories.PermissionRepository;
import com.bethelreport.backend.users.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

  @Mock
  private PermissionRepository permissionRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private BethelRepository bethelRepository;

  @Mock
  private PermissionMapper permissionMapper;

  @Mock
  private AuthorizationService authorizationService;

  @InjectMocks
  private PermissionService permissionService;

  private User user;
  private Bethel bethel;

  @BeforeEach
  void setUp() {
    Mockito.lenient().when(authorizationService.canAssignRole(any(), any(), any()))
        .thenReturn(true);
    user = new User();
    user.setId(1L);
    user.setActive(true);

    bethel = new Bethel();
    bethel.setId(10L);
  }

  @Test
  void shouldCreatePermissionForActiveMember() {
    CreatePermissionRequest request = new CreatePermissionRequest(1L, Role.MEMBRO_ATIVO, 10L);

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(permissionRepository.existsByUserId(1L)).thenReturn(false);
    when(bethelRepository.findById(10L)).thenReturn(Optional.of(bethel));
    Permission mappedPermission = new Permission();
    mappedPermission.setRole(request.role());
    mappedPermission.setActive(true);
    when(permissionMapper.toEntity(request)).thenReturn(mappedPermission);
    when(permissionRepository.save(any(Permission.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    stubPermissionResponse();
    PermissionResponse result = permissionService.create(request);

    assertNotNull(result);
    assertEquals(Role.MEMBRO_ATIVO, result.role());
    assertEquals(user.getId(), result.userId());
    assertEquals(bethel.getId(), result.bethelId());
    assertTrue(result.active());

    verify(permissionRepository).save(any(Permission.class));
  }

  @Test
  void deniesCreatingMemberInAnotherBethel() {
    CreatePermissionRequest request = new CreatePermissionRequest(1L, Role.MEMBRO_ATIVO, 11L);
    when(authorizationService.canAssignRole(any(), any(), any())).thenReturn(false);

    assertThrows(AccessDeniedException.class, () -> permissionService.create(request));
    verify(permissionRepository, never()).save(any(Permission.class));
  }

  @Test
  void deniesChangingGrandCouncilPermission() {
    Permission permission = new Permission();
    permission.setId(2L);
    permission.setRole(Role.GRANDE_CONSELHO);
    when(permissionRepository.findById(2L)).thenReturn(Optional.of(permission));
    when(authorizationService.canAssignRole(any(), any(), any())).thenReturn(false);

    assertThrows(AccessDeniedException.class,
        () -> permissionService.update(2L, new UpdatePermissionRequest(Role.MEMBRO_ATIVO, 10L)));
    verify(permissionRepository, never()).save(any(Permission.class));
  }

  @Test
  void activatesPermission() {
    Permission permission = new Permission();
    permission.setId(1L);
    permission.setActive(false);
    when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission));
    when(permissionRepository.save(any(Permission.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    stubPermissionResponse();

    PermissionResponse result = permissionService.activate(1L);

    assertTrue(result.active());
  }

  @Test
  void deactivatesPermission() {
    Permission permission = new Permission();
    permission.setId(1L);
    permission.setActive(true);
    when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission));
    when(permissionRepository.save(any(Permission.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    stubPermissionResponse();

    PermissionResponse result = permissionService.deactivate(1L);

    assertFalse(result.active());
  }

  @Test
  void findsPermissionById() {
    Permission permission = new Permission();
    permission.setId(1L);
    permission.setUser(user);
    permission.setRole(Role.MEMBRO_ATIVO);
    permission.setBethel(bethel);
    when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission));
    stubPermissionResponse();

    PermissionResponse result = permissionService.findById(1L);

    assertEquals(1L, result.id());
    assertEquals(user.getId(), result.userId());
    assertEquals(bethel.getId(), result.bethelId());
  }

  @Test
  void shouldRejectPermissionForUnknownUser() {
    CreatePermissionRequest request = new CreatePermissionRequest(99L, Role.MEMBRO_ATIVO, 10L);

    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () -> permissionService.create(request));

    verify(permissionRepository, never()).save(any(Permission.class));
  }

  @Test
  void shouldRejectPermissionForInactiveUser() {
    user.setActive(false);

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));

    CreatePermissionRequest request = new CreatePermissionRequest(1L, Role.MEMBRO_ATIVO, 10L);

    assertThrows(IllegalArgumentException.class, () -> permissionService.create(request));

    verify(permissionRepository, never()).save(any(Permission.class));
  }

  @Test
  void shouldRejectDuplicatePermission() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(permissionRepository.existsByUserId(1L)).thenReturn(true);

    CreatePermissionRequest request = new CreatePermissionRequest(1L, Role.MEMBRO_ATIVO, 10L);

    assertThrows(IllegalStateException.class, () -> permissionService.create(request));

    verify(permissionRepository, never()).save(any(Permission.class));
  }

  @Test
  void shouldRejectGlobalRoleWithBethel() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(permissionRepository.existsByUserId(1L)).thenReturn(false);

    CreatePermissionRequest request = new CreatePermissionRequest(1L, Role.GRANDE_CONSELHO, 10L);

    assertThrows(IllegalArgumentException.class, () -> permissionService.create(request));

    verify(permissionRepository, never()).save(any(Permission.class));
  }

  @Test
  void shouldRejectLocalRoleWithoutBethel() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(permissionRepository.existsByUserId(1L)).thenReturn(false);

    CreatePermissionRequest request = new CreatePermissionRequest(1L, Role.CONSELHO_DE_BETHEL,
        null);

    assertThrows(IllegalArgumentException.class, () -> permissionService.create(request));

    verify(permissionRepository, never()).save(any(Permission.class));
  }

  @Test
  void shouldRejectUnknownBethel() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(permissionRepository.existsByUserId(1L)).thenReturn(false);
    when(bethelRepository.findById(99L)).thenReturn(Optional.empty());

    CreatePermissionRequest request = new CreatePermissionRequest(1L, Role.MEMBRO_ATIVO, 99L);

    assertThrows(EntityNotFoundException.class, () -> permissionService.create(request));

    verify(permissionRepository, never()).save(any(Permission.class));
  }

  private void stubPermissionResponse() {
    when(permissionMapper.toResponse(any(Permission.class))).thenAnswer(invocation -> {
      Permission permission = invocation.getArgument(0);
      Long userId = permission.getUser() == null ? null : permission.getUser().getId();
      Long bethelId = permission.getBethel() == null ? null : permission.getBethel().getId();
      return new PermissionResponse(permission.getId(), userId, permission.getRole(), bethelId,
          permission.isActive());
    });
  }
}
