package com.bethelreport.backend.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bethelreport.backend.keycloak.client.KeycloakAdminClient;
import com.bethelreport.backend.keycloak.dtos.KeycloakUserResponse;
import com.bethelreport.backend.permissions.entities.Permission;
import com.bethelreport.backend.permissions.entities.Role;
import com.bethelreport.backend.permissions.repositories.PermissionRepository;
import com.bethelreport.backend.users.entities.User;
import com.bethelreport.backend.users.repositories.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AdminBootstrapTest {
  private static final String EMAIL = "admin@bethelreport.local";
  private final KeycloakAdminClient keycloak = mock(KeycloakAdminClient.class);
  private final UserRepository users = mock(UserRepository.class);
  private final PermissionRepository permissions = mock(PermissionRepository.class);
  private final AdminBootstrap bootstrap = new AdminBootstrap(keycloak, users, permissions, EMAIL);

  @BeforeEach
  void setUp() {
    when(keycloak.findUsersByEmail(EMAIL))
        .thenReturn(List.of(new KeycloakUserResponse("identity-id", EMAIL, EMAIL, true, true)));
    when(users.save(any(User.class))).thenAnswer(invocation -> {
      User user = invocation.getArgument(0);
      user.setId(1L);
      return user;
    });
  }

  @Test
  void createsLinkedActiveAdministratorAndPermission() {
    when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

    bootstrap.run(null);

    var userCaptor = ArgumentCaptor.forClass(User.class);
    verify(users).save(userCaptor.capture());
    assertEquals(EMAIL, userCaptor.getValue().getEmail());
    assertEquals("identity-id", userCaptor.getValue().getKeycloakUserId());
    assertTrue(userCaptor.getValue().isActive());
    var permissionCaptor = ArgumentCaptor.forClass(Permission.class);
    verify(permissions).save(permissionCaptor.capture());
    assertEquals(Role.GRANDE_CONSELHO, permissionCaptor.getValue().getRole());
    assertTrue(permissionCaptor.getValue().isActive());
  }

  @Test
  void rerunPreservesExistingProfileAndPermission() {
    User existing = new User();
    existing.setId(1L);
    existing.setName("Nome atualizado");
    existing.setActive(false);
    when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(existing));
    when(permissions.existsByUserId(1L)).thenReturn(true);

    bootstrap.run(null);
    bootstrap.run(null);

    assertEquals("Nome atualizado", existing.getName());
    assertEquals(false, existing.isActive());
    assertEquals("identity-id", existing.getKeycloakUserId());
    verify(permissions, never()).save(any());
  }

  @Test
  void refusesToReplaceAnotherIdentity() {
    User existing = new User();
    existing.setKeycloakUserId("another-identity");
    when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(existing));

    assertThrows(IllegalStateException.class, () -> bootstrap.run(null));

    verify(users, never()).save(any());
    verify(permissions, never()).save(any());
  }

  @Test
  void failsBeforeWritingWhenKeycloakBootstrapHasNotRun() {
    when(keycloak.findUsersByEmail(EMAIL)).thenReturn(List.of());

    assertThrows(IllegalStateException.class, () -> bootstrap.run(null));

    verify(users, never()).save(any());
  }
}
