package com.bethelreport.backend.permissions.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bethelreport.backend.config.SecurityConfig;
import com.bethelreport.backend.permissions.services.AuthorizationService;
import com.bethelreport.backend.permissions.dtos.CreatePermissionRequest;
import com.bethelreport.backend.permissions.dtos.PermissionResponse;
import com.bethelreport.backend.permissions.entities.Permission;
import com.bethelreport.backend.permissions.entities.Role;
import com.bethelreport.backend.permissions.repositories.PermissionRepository;
import com.bethelreport.backend.users.services.AdmissionService;
import com.bethelreport.backend.permissions.services.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PermissionController.class)
@Import({SecurityConfig.class, AuthorizationService.class})
class PermissionControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private PermissionService permissionService;

  @MockitoBean
  private AdmissionService admissionService;

  @MockitoBean
  private PermissionRepository permissionRepository;

  @MockitoBean
  private JwtDecoder jwtDecoder;

  @BeforeEach
  void setUp() {
    when(admissionService.admittedUser(any(Jwt.class))).thenReturn(true);
    Permission council = new Permission();
    council.setRole(Role.CONSELHO_DE_BETHEL);
    when(permissionRepository.findByUserEmail("council@example.com"))
        .thenReturn(java.util.Optional.of(council));
    Permission member = new Permission();
    member.setRole(Role.MEMBRO_ATIVO);
    when(permissionRepository.findByUserEmail("member@example.com"))
        .thenReturn(java.util.Optional.of(member));
    when(permissionRepository.findByUserEmail("unassigned@example.com"))
        .thenReturn(java.util.Optional.empty());
  }

  @Test
  void councilCanReachPermissionCreationEndpoint() throws Exception {
    when(permissionService.create(any(CreatePermissionRequest.class)))
        .thenReturn(new PermissionResponse(1L, 2L, Role.MEMBRO_ATIVO, 10L, true));

    mockMvc
        .perform(post("/api/permissions").with(jwtFor("council@example.com"))
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"userId":2,"role":"MEMBRO_ATIVO","bethelId":10}
                """))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("MEMBRO_ATIVO"));
    verify(permissionService).create(new CreatePermissionRequest(2L, Role.MEMBRO_ATIVO, 10L));
  }

  @Test
  void memberCannotCreatePermission() throws Exception {
    mockMvc.perform(post("/api/permissions").with(jwtFor("member@example.com"))
        .contentType(MediaType.APPLICATION_JSON).content("""
            {"userId":2,"role":"MEMBRO_ATIVO","bethelId":10}
            """)).andExpect(status().isForbidden());
  }

  @Test
  void userWithoutPermissionCannotCreatePermission() throws Exception {
    mockMvc.perform(post("/api/permissions").with(jwtFor("unassigned@example.com"))
        .contentType(MediaType.APPLICATION_JSON).content("""
            {"userId":2,"role":"MEMBRO_ATIVO","bethelId":10}
            """)).andExpect(status().isForbidden());
  }

  @Test
  void councilCannotMovePermissionToAnotherBethelOverHttp() throws Exception {
    when(permissionService.update(any(Long.class), any()))
        .thenThrow(new AccessDeniedException("Você não tem permissão para gerenciar este perfil"));

    mockMvc
        .perform(put("/api/permissions/2").with(jwtFor("council@example.com"))
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"role":"MEMBRO_ATIVO","bethelId":20}
                """))
        .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("Forbidden"));
  }

  @Test
  void councilCannotDeletePermissionFromAnotherBethelOverHttp() throws Exception {
    org.mockito.Mockito
        .doThrow(new AccessDeniedException("Você não tem permissão para gerenciar este perfil"))
        .when(permissionService).delete(2L);

    mockMvc.perform(delete("/api/permissions/2").with(jwtFor("council@example.com")))
        .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("Forbidden"));
  }

  private org.springframework.test.web.servlet.request.RequestPostProcessor jwtFor(String email) {
    return jwt().jwt(token -> token.claim("email", email));
  }
}
