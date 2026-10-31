package com.bethelreport.backend.permissions.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bethelreport.backend.bethel.entities.Bethel;
import com.bethelreport.backend.bethel.repositories.BethelRepository;
import com.bethelreport.backend.permissions.dtos.CreatePermissionRequest;
import com.bethelreport.backend.permissions.dtos.UpdatePermissionRequest;
import com.bethelreport.backend.permissions.entities.Permission;
import com.bethelreport.backend.permissions.entities.Role;
import com.bethelreport.backend.permissions.mappers.PermissionMapper;
import com.bethelreport.backend.permissions.repositories.PermissionRepository;
import com.bethelreport.backend.users.entities.User;
import com.bethelreport.backend.users.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class AdministratorServiceTest {
  @Mock
  private PermissionRepository permissions;
  @Mock
  private UserRepository users;
  @Mock
  private BethelRepository bethels;
  private PermissionService service;
  private User user;
  private Bethel bethel;
  private Permission existing;
  private Permission actor;

  @BeforeEach
  void setUp() {
    service = new PermissionService(permissions, users, bethels,
        Mappers.getMapper(PermissionMapper.class), new AuthorizationService(permissions));
    user = new User();
    user.setId(1L);
    user.setActive(true);
    bethel = new Bethel();
    bethel.setId(10L);
    existing = new Permission();
    existing.setId(2L);
    existing.setRole(Role.CONSELHO_DE_BETHEL);
    existing.setBethel(bethel);
    existing.setUser(user);
    actor = new Permission();
    actor.setRole(Role.GRANDE_CONSELHO);
    actor.setBethel(bethel);
    Jwt jwt = Jwt.withTokenValue("test").header("alg", "none").claim("email", "actor@example.com")
        .build();
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    when(permissions.findByUserEmail("actor@example.com")).thenReturn(Optional.of(actor));
  }

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void createsAdministrativeLink() {
    when(users.findById(1L)).thenReturn(Optional.of(user));
    when(bethels.findById(10L)).thenReturn(Optional.of(bethel));
    when(permissions.save(any())).thenAnswer(i -> i.getArgument(0));
    var response = service.create(request());
    assertEquals(1L, response.userId());
    assertEquals(10L, response.bethelId());
    assertEquals(Role.CONSELHO_DE_BETHEL, response.role());
    assertTrue(response.active());
  }

  @Test
  void movesAdministrativeLink() {
    when(permissions.findById(2L)).thenReturn(Optional.of(existing));
    Bethel destination = new Bethel();
    destination.setId(20L);
    when(bethels.findById(20L)).thenReturn(Optional.of(destination));
    when(permissions.save(any())).thenAnswer(i -> i.getArgument(0));
    assertEquals(20L,
        service.update(2L, new UpdatePermissionRequest(Role.CONSELHO_DE_BETHEL, 20L)).bethelId());
  }

  @Test
  void rejectsDuplicateLink() {
    when(users.findById(1L)).thenReturn(Optional.of(user));
    when(permissions.existsByUserId(1L)).thenReturn(true);
    assertThrows(IllegalStateException.class, () -> service.create(request()));
    verify(permissions, never()).save(any());
  }

  @Test
  void rejectsUnknownUser() {
    assertThrows(EntityNotFoundException.class, () -> service.create(request()));
    verify(permissions, never()).save(any());
  }

  @Test
  void rejectsInactiveUser() {
    user.setActive(false);
    when(users.findById(1L)).thenReturn(Optional.of(user));
    assertThrows(IllegalArgumentException.class, () -> service.create(request()));
    verify(permissions, never()).save(any());
  }

  @Test
  void rejectsUnknownBethel() {
    when(users.findById(1L)).thenReturn(Optional.of(user));
    assertThrows(EntityNotFoundException.class, () -> service.create(request()));
    verify(permissions, never()).save(any());
  }

  @Test
  void rejectsInactiveBethel() {
    bethel.setActive(false);
    when(users.findById(1L)).thenReturn(Optional.of(user));
    when(bethels.findById(10L)).thenReturn(Optional.of(bethel));
    assertThrows(IllegalArgumentException.class, () -> service.create(request()));
    verify(permissions, never()).save(any());
  }

  @Test
  void rejectsMovingToInactiveBethel() {
    bethel.setActive(false);
    when(permissions.findById(2L)).thenReturn(Optional.of(existing));
    when(bethels.findById(10L)).thenReturn(Optional.of(bethel));
    assertThrows(IllegalArgumentException.class,
        () -> service.update(2L, new UpdatePermissionRequest(Role.CONSELHO_DE_BETHEL, 10L)));
    verify(permissions, never()).save(any());
  }

  @Test
  void rejectsReactivatingLinkForInactiveUser() {
    user.setActive(false);
    existing.setActive(false);
    when(permissions.findById(2L)).thenReturn(Optional.of(existing));
    assertThrows(IllegalArgumentException.class, () -> service.activate(2L));
    assertFalse(existing.isActive());
  }

  @Test
  void listsActiveAdministrators() {
    when(bethels.existsById(10L)).thenReturn(true);
    when(permissions.findAllByBethelIdAndRoleAndActiveTrueAndUserActiveTrueOrderByIdAsc(10L,
        Role.CONSELHO_DE_BETHEL)).thenReturn(List.of(existing));
    assertEquals(1L, service.findAdministratorsByBethelId(10L).getFirst().userId());
  }

  @Test
  void listsEmptyBethel() {
    when(bethels.existsById(10L)).thenReturn(true);
    assertTrue(service.findAdministratorsByBethelId(10L).isEmpty());
  }

  @Test
  void rejectsListingUnknownBethel() {
    assertThrows(EntityNotFoundException.class, () -> service.findAdministratorsByBethelId(10L));
  }

  @Test
  void localCouncilCannotPromoteMemberToAdministrator() {
    actor.setRole(Role.CONSELHO_DE_BETHEL);
    existing.setRole(Role.MEMBRO_ATIVO);
    when(permissions.findById(2L)).thenReturn(Optional.of(existing));
    when(bethels.findById(10L)).thenReturn(Optional.of(bethel));
    assertThrows(AccessDeniedException.class,
        () -> service.update(2L, new UpdatePermissionRequest(Role.CONSELHO_DE_BETHEL, 10L)));
    assertEquals(Role.MEMBRO_ATIVO, existing.getRole());
  }

  @Test
  void inactiveGrandCouncilCannotCreateLink() {
    actor.setActive(false);
    assertThrows(AccessDeniedException.class, () -> service.create(request()));
  }

  private CreatePermissionRequest request() {
    return new CreatePermissionRequest(1L, Role.CONSELHO_DE_BETHEL, 10L);
  }
}
