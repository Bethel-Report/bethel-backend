package com.bethelreport.backend.invitations.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bethelreport.backend.bethel.entities.Bethel;
import com.bethelreport.backend.config.SecurityConfig;
import com.bethelreport.backend.exceptions.DuplicateEmailException;
import com.bethelreport.backend.exceptions.InvitationNotFoundException;
import com.bethelreport.backend.invitations.dtos.CreateInvitationRequest;
import com.bethelreport.backend.invitations.dtos.InvitationResponse;
import com.bethelreport.backend.invitations.entities.InvitationStatus;
import com.bethelreport.backend.invitations.services.InvitationService;
import com.bethelreport.backend.permissions.entities.Permission;
import com.bethelreport.backend.permissions.entities.Role;
import com.bethelreport.backend.permissions.repositories.PermissionRepository;
import com.bethelreport.backend.permissions.services.AuthorizationService;
import com.bethelreport.backend.users.services.AdmissionService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(InvitationController.class)
@Import({SecurityConfig.class, AuthorizationService.class})
class InvitationControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private InvitationService invitationService;

  @MockitoBean
  private AdmissionService admissionService;

  @MockitoBean
  private PermissionRepository permissionRepository;

  @MockitoBean
  private JwtDecoder jwtDecoder;

  @BeforeEach
  void setUp() {
    when(admissionService.admittedUser(any())).thenReturn(true);
    Permission councilPermission = new Permission();
    councilPermission.setRole(Role.CONSELHO_DE_BETHEL);
    Bethel bethel = new Bethel();
    bethel.setId(10L);
    councilPermission.setBethel(bethel);
    when(permissionRepository.findByUserEmail("council@example.com"))
        .thenReturn(java.util.Optional.of(councilPermission));
  }

  private org.springframework.test.web.servlet.request.RequestPostProcessor councilJwt() {
    return jwt().jwt(token -> token.claim("email", "council@example.com"));
  }

  @Test
  void createInvitationReturnsCreated() throws Exception {
    CreateInvitationRequest request = new CreateInvitationRequest("invited@example.com", 10L);
    InvitationResponse response = new InvitationResponse(1L, "invited@example.com",
        InvitationStatus.PENDING, LocalDateTime.of(2026, 10, 31, 10, 0), 10L);

    when(invitationService.createInvitation(any(CreateInvitationRequest.class),
        any(Authentication.class))).thenReturn(response);

    mockMvc
        .perform(post("/api/invitations").with(councilJwt()).contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.email").value("invited@example.com"))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.bethelId").value(10));
  }

  @Test
  void createInvitationWithInvalidEmailReturnsBadRequest() throws Exception {
    CreateInvitationRequest invalid = new CreateInvitationRequest("", 10L);

    mockMvc
        .perform(post("/api/invitations").with(councilJwt()).contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalid)))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors").exists());
  }

  @Test
  void createInvitationWithDuplicateEmailReturnsConflict() throws Exception {
    CreateInvitationRequest request = new CreateInvitationRequest("dup@example.com", 10L);

    when(invitationService.createInvitation(any(CreateInvitationRequest.class),
        any(Authentication.class))).thenThrow(new DuplicateEmailException("dup@example.com"));

    mockMvc
        .perform(post("/api/invitations").with(councilJwt()).contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict()).andExpect(jsonPath("$.message").exists());
  }

  @Test
  void createInvitationForAnotherBethelReturnsForbidden() throws Exception {
    CreateInvitationRequest request = new CreateInvitationRequest("new@example.com", 20L);

    when(invitationService.createInvitation(any(CreateInvitationRequest.class),
        any(Authentication.class)))
        .thenThrow(new AccessDeniedException("Você não pode convidar para este Bethel."));

    mockMvc.perform(post("/api/invitations").with(councilJwt())
        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  void listInvitationsReturnsOk() throws Exception {
    InvitationResponse response = new InvitationResponse(1L, "a@example.com",
        InvitationStatus.PENDING, LocalDateTime.of(2026, 10, 31, 10, 0), 10L);

    when(invitationService.listInvitations()).thenReturn(List.of(response));

    mockMvc.perform(get("/api/invitations").with(councilJwt())).andExpect(status().isOk())
        .andExpect(jsonPath("$[0].email").value("a@example.com"))
        .andExpect(jsonPath("$[0].status").value("PENDING"))
        .andExpect(jsonPath("$[0].bethelId").value(10));
  }

  @Test
  void getInvitationByIdReturnsOk() throws Exception {
    InvitationResponse response = new InvitationResponse(1L, "a@example.com",
        InvitationStatus.PENDING, LocalDateTime.of(2026, 10, 31, 10, 0), 10L);

    when(invitationService.getInvitationById(eq(1L), any(Authentication.class)))
        .thenReturn(response);

    mockMvc.perform(get("/api/invitations/1").with(councilJwt())).andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.email").value("a@example.com"));
  }

  @Test
  void getInvitationByIdNotFoundReturnsNotFound() throws Exception {
    when(invitationService.getInvitationById(eq(99L), any(Authentication.class)))
        .thenThrow(new InvitationNotFoundException(99L));

    mockMvc.perform(get("/api/invitations/99").with(councilJwt())).andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  void getInvitationFromAnotherBethelReturnsForbidden() throws Exception {
    when(invitationService.getInvitationById(eq(5L), any(Authentication.class)))
        .thenThrow(new AccessDeniedException("Você não pode gerenciar este convite."));

    mockMvc.perform(get("/api/invitations/5").with(councilJwt())).andExpect(status().isForbidden());
  }

  @Test
  void revokeInvitationReturnsNoContent() throws Exception {
    mockMvc.perform(delete("/api/invitations/1").with(councilJwt()))
        .andExpect(status().isNoContent());

    verify(invitationService).revokeInvitation(eq(1L), any(Authentication.class));
  }

  @Test
  void revokeInvitationNotFoundReturnsNotFound() throws Exception {
    doThrow(new InvitationNotFoundException(99L)).when(invitationService).revokeInvitation(eq(99L),
        any(Authentication.class));

    mockMvc.perform(delete("/api/invitations/99").with(councilJwt()))
        .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").exists());
  }

  @Test
  void unauthenticatedRequestReturnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/invitations")).andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("Unauthorized"));
  }

  @Test
  void inactiveUserIsDeniedAccess() throws Exception {
    reset(admissionService);
    when(admissionService.admittedUser(any())).thenReturn(false);

    mockMvc.perform(get("/api/invitations").with(jwt())).andExpect(status().isForbidden());
  }

  @Test
  void memberCannotCreateInvitation() throws Exception {
    Permission memberPermission = new Permission();
    memberPermission.setRole(Role.MEMBRO_ATIVO);
    when(permissionRepository.findByUserEmail("member@example.com"))
        .thenReturn(java.util.Optional.of(memberPermission));

    mockMvc
        .perform(post("/api/invitations")
            .with(jwt().jwt(token -> token.claim("email", "member@example.com")))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper
                .writeValueAsString(new CreateInvitationRequest("new@example.com", 10L))))
        .andExpect(status().isForbidden());
  }
}
