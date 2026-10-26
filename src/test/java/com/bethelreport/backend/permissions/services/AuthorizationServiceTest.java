package com.bethelreport.backend.permissions.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

import com.bethelreport.backend.bethel.entities.Bethel;
import com.bethelreport.backend.permissions.entities.Permission;
import com.bethelreport.backend.permissions.entities.Role;
import com.bethelreport.backend.permissions.repositories.PermissionRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

  private static final Long COUNCIL_BETHEL_ID = 10L;
  private static final Long OTHER_BETHEL_ID = 20L;

  @Mock
  private PermissionRepository permissionRepository;

  private AuthorizationService authorizationService;
  private Authentication council;
  private Authentication member;
  private Authentication grandCouncil;

  @BeforeEach
  void setUp() {
    authorizationService = new AuthorizationService(permissionRepository);
    council = authentication("council@example.com");
    member = authentication("member@example.com");
    grandCouncil = authentication("gc@example.com");
    lenient().when(permissionRepository.findByUserEmail("council@example.com"))
        .thenReturn(Optional.of(permission(Role.CONSELHO_DE_BETHEL, COUNCIL_BETHEL_ID)));
    lenient().when(permissionRepository.findByUserEmail("member@example.com"))
        .thenReturn(Optional.of(permission(Role.MEMBRO_ATIVO, COUNCIL_BETHEL_ID)));
    lenient().when(permissionRepository.findByUserEmail("gc@example.com"))
        .thenReturn(Optional.of(permission(Role.GRANDE_CONSELHO, null)));
    lenient().when(permissionRepository.findByUserEmail("unassigned@example.com"))
        .thenReturn(Optional.empty());
  }

  @Test
  void councilCanAssignLocalMemberInOwnBethel() {
    assertTrue(authorizationService.canAssignRole(council, Role.MEMBRO_ATIVO, COUNCIL_BETHEL_ID));
  }

  @Test
  void councilCannotAssignRoleInAnotherBethel() {
    assertFalse(authorizationService.canAssignRole(council, Role.MEMBRO_ATIVO, OTHER_BETHEL_ID));
  }

  @Test
  void councilCannotAssignGrandCouncilRole() {
    assertFalse(authorizationService.canAssignRole(council, Role.GRANDE_CONSELHO, null));
  }

  @Test
  void grandCouncilCanAssignLocalRoleWithBethel() {
    assertTrue(authorizationService.canAssignRole(grandCouncil, Role.CONSELHO_DE_BETHEL,
        COUNCIL_BETHEL_ID));
  }

  @Test
  void grandCouncilCanAssignGlobalRoleWithoutBethel() {
    assertTrue(authorizationService.canAssignRole(grandCouncil, Role.GRANDE_CONSELHO, null));
  }

  @Test
  void grandCouncilCannotAssignGlobalRoleWithBethel() {
    assertFalse(
        authorizationService.canAssignRole(grandCouncil, Role.GRANDE_CONSELHO, COUNCIL_BETHEL_ID));
  }

  @Test
  void councilCanAccessOwnBethelButNotOtherBethel() {
    assertTrue(authorizationService.canAccessBethel(COUNCIL_BETHEL_ID, council));
    assertFalse(authorizationService.canAccessBethel(OTHER_BETHEL_ID, council));
  }

  @Test
  void grandCouncilCanAccessAnyBethel() {
    assertTrue(authorizationService.canAccessBethel(OTHER_BETHEL_ID, grandCouncil));
  }

  @Test
  void memberCanReadOwnBethelButCannotManageIt() {
    assertTrue(authorizationService.canAccessBethel(COUNCIL_BETHEL_ID, member));
    assertFalse(authorizationService.canManageBethel(COUNCIL_BETHEL_ID, member));
  }

  @Test
  void unassignedUserCannotAccessProtectedBethel() {
    assertFalse(authorizationService.canAccessBethel(OTHER_BETHEL_ID,
        authentication("unassigned@example.com")));
  }

  private Authentication authentication(String email) {
    Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("email", email).build();
    return new JwtAuthenticationToken(jwt);
  }

  private Permission permission(Role role, Long bethelId) {
    Permission permission = new Permission();
    permission.setRole(role);
    if (bethelId != null) {
      Bethel bethel = new Bethel();
      bethel.setId(bethelId);
      permission.setBethel(bethel);
    }
    return permission;
  }
}
