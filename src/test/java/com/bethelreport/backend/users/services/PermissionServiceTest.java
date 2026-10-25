package com.bethelreport.backend.users.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.bethelreport.backend.bethel.entities.Bethel;
import com.bethelreport.backend.bethel.repositories.BethelRepository;
import com.bethelreport.backend.users.dtos.CreatePermissionRequest;
import com.bethelreport.backend.users.entities.Permission;
import com.bethelreport.backend.users.entities.Role;
import com.bethelreport.backend.users.entities.User;
import com.bethelreport.backend.users.mappers.PermissionMapper;
import com.bethelreport.backend.users.repositories.PermissionRepository;
import com.bethelreport.backend.users.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

  @InjectMocks
  private PermissionService permissionService;

  private User user;
  private Bethel bethel;

  @BeforeEach
  void setUp() {
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
    when(permissionMapper.toEntity(request)).thenReturn(mappedPermission);
    when(permissionRepository.save(any(Permission.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Permission result = permissionService.create(request);

    assertNotNull(result);
    assertEquals(Role.MEMBRO_ATIVO, result.getRole());
    assertEquals(user, result.getUser());
    assertEquals(bethel, result.getBethel());

    verify(permissionRepository).save(any(Permission.class));
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
}
